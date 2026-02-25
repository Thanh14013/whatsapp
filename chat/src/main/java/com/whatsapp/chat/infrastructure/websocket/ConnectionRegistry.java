package com.whatsapp.chat.infrastructure.websocket;

import com.whatsapp.chat.infrastructure.cache.InboxCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connection Registry
 *
 * Listens to STOMP session lifecycle events (connect / disconnect)
 * and keeps the {@link WebSocketSessionManager} in sync.
 *
 * On connect:
 *  1. Registers the session in {@link WebSocketSessionManager}
 *  2. Marks the user as online in Redis via {@link InboxCacheService}
 *
 * On disconnect:
 *  1. Removes the session from {@link WebSocketSessionManager}
 *  2. Marks the user as offline in Redis
 *
 * The user identity is read from the {@link Principal} attached to the
 * STOMP session by the security layer.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionRegistry {

    private final WebSocketSessionManager sessionManager;
    private final InboxCacheService        inboxCacheService;

    /** sessionId → userId  (used on disconnect where Principal may be gone) */
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------
    // Event listeners
    // ---------------------------------------------------------------

    /**
     * Called when a STOMP client successfully connects.
     *
     * @param event the Spring WebSocket connected event
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();

        if (principal == null) {
            log.warn("WebSocket session connected without authenticated principal: sessionId={}", sessionId);
            return;
        }

        String userId = principal.getName();
        sessionUserMap.put(sessionId, userId);
        sessionManager.addSession(userId, sessionId);
        inboxCacheService.addOnlineUser(userId);

        log.info("WebSocket connected: userId={} sessionId={}", userId, sessionId);
    }

    /**
     * Called when a STOMP client disconnects (graceful or timeout).
     *
     * @param event the Spring WebSocket disconnect event
     */
    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        String userId = sessionUserMap.remove(sessionId);
        if (userId == null) {
            log.debug("Disconnect event for unknown sessionId={}", sessionId);
            return;
        }

        sessionManager.removeSession(userId, sessionId);

        // Only mark offline if no more sessions remain
        if (!sessionManager.isUserConnected(userId)) {
            inboxCacheService.removeOnlineUser(userId);
            log.info("User went offline: userId={}", userId);
        }

        log.info("WebSocket disconnected: userId={} sessionId={}", userId, sessionId);
    }

    // ---------------------------------------------------------------
    // Query helpers
    // ---------------------------------------------------------------

    /**
     * Resolve the user ID for a given session ID.
     *
     * @param sessionId STOMP session ID
     * @return user ID, or {@code null} if not found
     */
    public String getUserIdForSession(String sessionId) {
        return sessionUserMap.get(sessionId);
    }
}

