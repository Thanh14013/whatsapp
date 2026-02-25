package com.whatsapp.chat.interfaces.websocket;

import com.whatsapp.chat.application.dto.MessageDto;
import com.whatsapp.chat.application.dto.MessageReceivedResponse;
import com.whatsapp.chat.application.service.MessageQueryService;
import com.whatsapp.chat.infrastructure.websocket.WebSocketSessionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

/**
 * Message Event Handler
 *
 * Bridges RabbitMQ message events to connected WebSocket clients.
 *
 * Responsibilities:
 * 1. React to STOMP subscription requests (e.g. client subscribes to
 *    a conversation topic → send missed messages since last sync).
 * 2. Forward domain events received from RabbitMQ over WebSocket.
 *
 * This handler works alongside {@link ChatWebSocketController} to
 * decouple event routing from message sending logic.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MessageEventHandler {

    private final MessageQueryService     messageQueryService;
    private final WebSocketSessionManager sessionManager;

    // ---------------------------------------------------------------
    // STOMP subscription hook
    // ---------------------------------------------------------------

    /**
     * Handle a client request to subscribe to a conversation.
     *
     * When a client subscribes, we send the last N messages so the
     * UI can render the conversation without an extra REST call.
     *
     * Expected payload:
     * <pre>
     * { "conversationId": "conv-abc", "page": 0, "size": 50 }
     * </pre>
     */
    @MessageMapping("/chat.subscribe")
    public void handleSubscribe(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        String userId        = principal.getName();
        String conversationId = payload.get("conversationId");
        if (conversationId == null) return;

        int page = parseIntOrDefault(payload.get("page"), 0);
        int size = parseIntOrDefault(payload.get("size"), 50);

        try {
            java.util.List<MessageDto> history =
                    messageQueryService.getConversationHistory(conversationId, page, size);

            // Send history back to the requesting user
            sessionManager.sendToUser(userId, "/queue/history",
                    Map.of(
                            "conversationId", conversationId,
                            "page", page,
                            "size", history.size(),
                            "messages", history
                    ));

            log.debug("Sent {} history messages for conversation {} to user {}",
                    history.size(), conversationId, userId);
        } catch (Exception e) {
            log.error("Error handling chat.subscribe for conversation {}: {}", conversationId, e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Status event push
    // ---------------------------------------------------------------

    /**
     * Handle an "online status" query from a client.
     *
     * Expected payload:
     * <pre>{ "targetUserId": "user-456" }</pre>
     *
     * Response is sent to {@code /user/{userId}/queue/status}.
     */
    @MessageMapping("/chat.status")
    public void handleStatusQuery(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        String requesterId   = principal.getName();
        String targetUserId  = payload.get("targetUserId");
        if (targetUserId == null) return;

        boolean online = sessionManager.isUserConnected(targetUserId);

        sessionManager.sendToUser(requesterId, "/queue/status",
                Map.of(
                        "userId",    targetUserId,
                        "online",    online,
                        "timestamp", Instant.now().toString()
                ));

        log.debug("Status query: {} is {} (requested by {})",
                targetUserId, online ? "ONLINE" : "OFFLINE", requesterId);
    }

    // ---------------------------------------------------------------
    // Unread count
    // ---------------------------------------------------------------

    /**
     * Handle a request for the current undelivered message count (badge).
     *
     * Expected payload:
     * <pre>{ "userId": "user-123" }</pre>
     */
    @MessageMapping("/chat.unreadCount")
    public void handleUnreadCount(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        String userId = payload.getOrDefault("userId", principal.getName());

        long count = messageQueryService.countUndelivered(userId);

        sessionManager.sendToUser(userId, "/queue/unreadCount",
                Map.of("unreadCount", count, "timestamp", Instant.now().toString()));

        log.debug("Sent unread count ({}) to user {}", count, userId);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

