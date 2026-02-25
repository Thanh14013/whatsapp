package com.whatsapp.chat.infrastructure.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Session Manager
 *
 * Manages the mapping between user IDs and their active STOMP sessions.
 * Used to:
 *  - Check whether a user is currently connected
 *  - Push server-initiated messages to a specific user
 *  - Maintain session metadata (e.g. subscribed topics)
 *
 * Thread-safety: all internal maps use {@link ConcurrentHashMap}.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    private final SimpMessagingTemplate messagingTemplate;

    /** userId → set of active session IDs */
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    public WebSocketSessionManager(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // ---------------------------------------------------------------
    // Session lifecycle
    // ---------------------------------------------------------------

    /**
     * Register a new session for the given user.
     *
     * @param userId    the authenticated user's ID
     * @param sessionId the STOMP session ID
     */
    public void addSession(String userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.debug("Session registered: userId={} sessionId={} (total sessions for user: {})",
                userId, sessionId, userSessions.get(userId).size());
    }

    /**
     * Remove a session (user disconnected or session expired).
     *
     * @param userId    the authenticated user's ID
     * @param sessionId the STOMP session ID
     */
    public void removeSession(String userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
                log.debug("All sessions removed for user: {}", userId);
            }
        }
        log.debug("Session removed: userId={} sessionId={}", userId, sessionId);
    }

    // ---------------------------------------------------------------
    // Query
    // ---------------------------------------------------------------

    /**
     * Check whether a user has at least one active session.
     *
     * @param userId the user's ID
     * @return {@code true} if the user is currently connected
     */
    public boolean isUserConnected(String userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Get the active session IDs for a user.
     *
     * @param userId the user's ID
     * @return set of session IDs (empty set if offline)
     */
    public Set<String> getUserSessions(String userId) {
        return userSessions.getOrDefault(userId, Set.of());
    }

    /**
     * Return the total number of distinct connected users.
     *
     * @return connected user count
     */
    public int getConnectedUserCount() {
        return userSessions.size();
    }

    // ---------------------------------------------------------------
    // Messaging
    // ---------------------------------------------------------------

    /**
     * Send a payload to all sessions of a specific user.
     *
     * The destination follows the STOMP user-destination convention:
     *   {@code /user/{userId}{destination}}
     *
     * @param userId      the target user's ID
     * @param destination STOMP destination suffix (e.g. {@code /queue/messages})
     * @param payload     the message payload (will be serialised to JSON)
     */
    public void sendToUser(String userId, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(userId, destination, payload);
            log.debug("Sent message to user {} at destination {}", userId, destination);
        } catch (Exception e) {
            log.error("Failed to send message to user {} at {}: {}", userId, destination, e.getMessage(), e);
        }
    }

    /**
     * Broadcast a payload to a topic (all subscribers).
     *
     * @param topic   STOMP topic (e.g. {@code /topic/conversation.abc123})
     * @param payload the message payload
     */
    public void broadcast(String topic, Object payload) {
        try {
            messagingTemplate.convertAndSend(topic, payload);
            log.debug("Broadcast message to topic {}", topic);
        } catch (Exception e) {
            log.error("Failed to broadcast to topic {}: {}", topic, e.getMessage(), e);
        }
    }
}

