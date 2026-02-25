package com.whatsapp.chat.interfaces.websocket;

import com.whatsapp.chat.application.dto.MessageDto;
import com.whatsapp.chat.application.dto.MessageReceivedResponse;
import com.whatsapp.chat.application.dto.SendMessageRequest;
import com.whatsapp.chat.application.service.ChatApplicationService;
import com.whatsapp.chat.infrastructure.websocket.WebSocketSessionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

/**
 * Chat WebSocket Controller
 *
 * Exposes STOMP message-mapping endpoints for the chat interface.
 *
 * Client destinations (prefixed with /app):
 *  /app/chat.message        – send a new chat message
 *  /app/chat.delivered      – acknowledge message delivery
 *  /app/chat.read           – mark a message as read
 *
 * Server push destinations:
 *  /user/{userId}/queue/messages  – incoming messages
 *  /user/{userId}/queue/receipts  – delivery / read receipts
 *  /topic/conversation.{id}       – broadcast in a group conversation
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatApplicationService  chatService;
    private final WebSocketSessionManager sessionManager;

    // ---------------------------------------------------------------
    // Send message
    // ---------------------------------------------------------------

    /**
     * Handle a new message from a STOMP client.
     *
     * Expected payload:
     * <pre>
     * {
     *   "conversationId": "conv-abc",
     *   "receiverId":     "user-456",
     *   "content":        "Hello!",
     *   "contentType":    "TEXT",
     *   "replyToMessageId": null
     * }
     * </pre>
     *
     * The sender is resolved from the authenticated STOMP {@link Principal}.
     */
    @MessageMapping("/chat.message")
    public void sendMessage(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            log.warn("Unauthenticated sendMessage attempt");
            return;
        }

        String senderId      = principal.getName();
        String receiverId    = payload.get("receiverId");
        String conversationId = payload.get("conversationId");
        String content        = payload.get("content");
        String contentType    = payload.getOrDefault("contentType", "TEXT");
        String replyToId      = payload.get("replyToMessageId");

        if (receiverId == null || conversationId == null || content == null) {
            log.warn("Incomplete chat.message payload from {}: {}", senderId, payload);
            return;
        }

        try {
            SendMessageRequest request = SendMessageRequest.builder()
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .conversationId(conversationId)
                    .content(content)
                    .contentType(SendMessageRequest.ContentType.valueOf(contentType))
                    .replyToMessageId(replyToId)
                    .build();

            MessageDto sent = chatService.sendMessage(request);

            // Build real-time response
            MessageReceivedResponse response = MessageReceivedResponse.builder()
                    .messageId(sent.getId())
                    .conversationId(sent.getConversationId())
                    .senderId(sent.getSenderId())
                    .receiverId(sent.getReceiverId())
                    .contentType(sent.getContentType())
                    .content(sent.getContent())
                    .mediaUrl(sent.getMediaUrl())
                    .status(sent.getStatus())
                    .sentAt(sent.getSentAt())
                    .replyToMessageId(sent.getReplyToMessageId())
                    .deleted(sent.isDeleted())
                    .serverTimestamp(Instant.now())
                    .build();

            // Push to receiver (if online)
            if (sessionManager.isUserConnected(receiverId)) {
                sessionManager.sendToUser(receiverId, "/queue/messages", response);
            }

            // Echo back to sender (for multi-device sync)
            sessionManager.sendToUser(senderId, "/queue/messages", response);

            log.info("WebSocket message sent: id={} from={} to={}", sent.getId(), senderId, receiverId);

        } catch (Exception e) {
            log.error("Error in chat.message from {}: {}", senderId, e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Delivery acknowledgement
    // ---------------------------------------------------------------

    /**
     * Handle a delivery acknowledgement from the recipient.
     *
     * Expected payload:
     * <pre>{ "messageId": "msg-123" }</pre>
     */
    @MessageMapping("/chat.delivered")
    public void markDelivered(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        String userId    = principal.getName();
        String messageId = payload.get("messageId");
        if (messageId == null) return;

        try {
            MessageDto updated = chatService.markAsDelivered(messageId, userId);
            // Notify the original sender
            sessionManager.sendToUser(updated.getSenderId(), "/queue/receipts",
                    Map.of("type", "DELIVERED", "messageId", messageId, "timestamp", Instant.now().toString()));
            log.debug("Delivery ACK: messageId={} by userId={}", messageId, userId);
        } catch (Exception e) {
            log.error("Error in chat.delivered: {}", e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Read receipt
    // ---------------------------------------------------------------

    /**
     * Handle a read receipt from the recipient.
     *
     * Expected payload:
     * <pre>{ "messageId": "msg-123" }</pre>
     */
    @MessageMapping("/chat.read")
    public void markRead(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        String userId    = principal.getName();
        String messageId = payload.get("messageId");
        if (messageId == null) return;

        try {
            MessageDto updated = chatService.markAsRead(messageId, userId);
            // Notify the original sender
            sessionManager.sendToUser(updated.getSenderId(), "/queue/receipts",
                    Map.of("type", "READ", "messageId", messageId, "timestamp", Instant.now().toString()));
            log.debug("Read receipt: messageId={} by userId={}", messageId, userId);
        } catch (Exception e) {
            log.error("Error in chat.read: {}", e.getMessage(), e);
        }
    }
}

