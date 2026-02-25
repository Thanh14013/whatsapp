package com.whatsapp.chat.infrastructure.websocket;

import com.whatsapp.chat.application.dto.MessageDto;
import com.whatsapp.chat.application.service.ChatApplicationService;
import com.whatsapp.chat.domain.service.DeliveryTrackingService;
import com.whatsapp.chat.infrastructure.cache.UndeliveredMessageCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * WebSocket Handler
 *
 * STOMP message handler for real-time chat interactions.
 *
 * Supported inbound destinations:
 *  /app/chat.sendMessage  – client sends a message
 *  /app/chat.markRead     – client marks a conversation as read
 *  /app/chat.sync         – client requests inbox sync on reconnect
 *
 * On session connect, this handler flushes the user's undelivered
 * message inbox so they receive messages sent while offline.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketHandler {

    private final ChatApplicationService  chatService;
    private final DeliveryTrackingService deliveryTrackingService;
    private final UndeliveredMessageCache undeliveredCache;
    private final WebSocketSessionManager sessionManager;

    // ---------------------------------------------------------------
    // Connect hook – deliver offline messages
    // ---------------------------------------------------------------

    /**
     * When a new STOMP session is established, flush any messages
     * that were sent while the user was offline.
     */
    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal == null) return;

        String userId = principal.getName();

        // Deliver messages that arrived while the user was offline
        int delivered = deliveryTrackingService.deliverPendingMessages(userId);
        if (delivered > 0) {
            log.info("Flushed {} offline messages to user {}", delivered, userId);
        }

        // Also push cached undelivered messages directly over the new WebSocket
        List<MessageDto> cachedMessages = undeliveredCache.popAllMessages(userId);
        for (MessageDto msg : cachedMessages) {
            sessionManager.sendToUser(userId, "/queue/messages", msg);
        }
        if (!cachedMessages.isEmpty()) {
            log.info("Pushed {} cached inbox messages to user {}", cachedMessages.size(), userId);
        }
    }

    // ---------------------------------------------------------------
    // Inbound message handlers
    // ---------------------------------------------------------------

    /**
     * Handle an incoming chat message from a STOMP client.
     *
     * Expected payload fields:
     * <pre>
     * {
     *   "senderId":       "user-123",
     *   "receiverId":     "user-456",
     *   "conversationId": "conv-789",
     *   "content":        "Hello!",
     *   "contentType":    "TEXT",
     *   "replyToMessageId": null
     * }
     * </pre>
     */
    @MessageMapping("/chat.sendMessage")
    public void handleSendMessage(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        String authenticatedSenderId = principal != null ? principal.getName() : null;

        String senderId        = payload.getOrDefault("senderId", authenticatedSenderId);
        String receiverId      = payload.get("receiverId");
        String conversationId  = payload.get("conversationId");
        String content         = payload.get("content");
        String contentType     = payload.getOrDefault("contentType", "TEXT");
        String replyToId       = payload.get("replyToMessageId");

        if (receiverId == null || conversationId == null || content == null) {
            log.warn("Incomplete sendMessage payload from user {}: {}", senderId, payload);
            return;
        }

        try {
            com.whatsapp.chat.application.dto.SendMessageRequest request =
                    com.whatsapp.chat.application.dto.SendMessageRequest.builder()
                            .senderId(senderId)
                            .receiverId(receiverId)
                            .conversationId(conversationId)
                            .content(content)
                            .contentType(com.whatsapp.chat.application.dto.SendMessageRequest.ContentType
                                    .valueOf(contentType))
                            .replyToMessageId(replyToId)
                            .build();

            MessageDto sent = chatService.sendMessage(request);
            log.info("WebSocket message sent: id={} from={} to={}", sent.getId(), senderId, receiverId);
        } catch (Exception e) {
            log.error("Error handling WebSocket sendMessage from {}: {}", senderId, e.getMessage(), e);
        }
    }

    /**
     * Handle a "mark conversation as read" request from a STOMP client.
     *
     * Expected payload fields:
     * <pre>
     * { "conversationId": "conv-789", "userId": "user-123" }
     * </pre>
     */
    @MessageMapping("/chat.markRead")
    public void handleMarkRead(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        String userId        = payload.getOrDefault("userId",
                principal != null ? principal.getName() : null);
        String conversationId = payload.get("conversationId");

        if (userId == null || conversationId == null) {
            log.warn("Incomplete markRead payload: {}", payload);
            return;
        }

        try {
            chatService.markAsRead(conversationId, userId);
            log.debug("Marked conversation {} as read by {}", conversationId, userId);
        } catch (Exception e) {
            log.error("Error handling markRead for conversation {}: {}", conversationId, e.getMessage(), e);
        }
    }

    /**
     * Handle an inbox-sync request from a reconnecting STOMP client.
     *
     * Expected payload fields:
     * <pre>
     * { "userId": "user-123" }
     * </pre>
     */
    @MessageMapping("/chat.sync")
    public void handleSync(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        String userId = payload.getOrDefault("userId",
                principal != null ? principal.getName() : null);

        if (userId == null) return;

        List<MessageDto> pending = undeliveredCache.popAllMessages(userId);
        for (MessageDto msg : pending) {
            sessionManager.sendToUser(userId, "/queue/messages", msg);
        }
        log.info("Sync: pushed {} messages to user {}", pending.size(), userId);
    }
}

