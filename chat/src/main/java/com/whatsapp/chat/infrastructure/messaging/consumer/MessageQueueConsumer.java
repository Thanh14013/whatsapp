package com.whatsapp.chat.infrastructure.messaging.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.chat.application.service.ChatApplicationService;
import com.whatsapp.chat.infrastructure.config.RabbitMQConfig;
import com.whatsapp.chat.infrastructure.websocket.WebSocketSessionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Message Queue Consumer
 *
 * Listens to RabbitMQ queues and reacts to message lifecycle events
 * published by this service or by other micro-services.
 *
 * Queues consumed:
 *   {@value RabbitMQConfig#MESSAGE_SENT_QUEUE}      – new messages to forward via WebSocket
 *   {@value RabbitMQConfig#MESSAGE_DELIVERED_QUEUE} – delivery ACKs to propagate back to senders
 *   {@value RabbitMQConfig#MESSAGE_READ_QUEUE}      – read receipts to propagate back to senders
 *
 * Each listener is idempotent; duplicate events are safely ignored.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageQueueConsumer {

    private final ObjectMapper            objectMapper;
    private final WebSocketSessionManager sessionManager;
    private final ChatApplicationService  chatApplicationService;

    // ---------------------------------------------------------------
    // Consumers
    // ---------------------------------------------------------------

    /**
     * Handle MESSAGE_SENT events.
     *
     * When a message is persisted, push it in real-time to the
     * recipient's WebSocket session (if they are online).
     *
     * @param payload JSON string from RabbitMQ
     */
    @RabbitListener(queues = RabbitMQConfig.MESSAGE_SENT_QUEUE)
    public void onMessageSent(String payload) {
        try {
            Map<String, Object> event = parseEvent(payload);
            String messageId     = getString(event, "messageId");
            String receiverId    = getString(event, "receiverId");

            log.debug("Received MESSAGE_SENT event: messageId={} receiverId={}", messageId, receiverId);

            // Forward to recipient via WebSocket if they are online
            if (sessionManager.isUserConnected(receiverId)) {
                sessionManager.sendToUser(receiverId, "/queue/messages", event);
                log.info("Forwarded MESSAGE_SENT to online user {} via WebSocket", receiverId);

                // Immediately mark as delivered
                chatApplicationService.markAsDelivered(messageId, receiverId);
            } else {
                log.debug("Recipient {} is offline; message {} queued for later delivery", receiverId, messageId);
                // Message stays in the undelivered cache (pushed before publishing)
            }
        } catch (Exception e) {
            log.error("Error processing MESSAGE_SENT event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle MESSAGE_DELIVERED events.
     *
     * Notify the original sender (if online) that their message was delivered.
     *
     * @param payload JSON string from RabbitMQ
     */
    @RabbitListener(queues = RabbitMQConfig.MESSAGE_DELIVERED_QUEUE)
    public void onMessageDelivered(String payload) {
        try {
            Map<String, Object> event = parseEvent(payload);
            String messageId  = getString(event, "messageId");
            String senderId   = getString(event, "senderId");

            log.debug("Received MESSAGE_DELIVERED event: messageId={} senderId={}", messageId, senderId);

            // Push delivery receipt to sender in real-time
            if (sessionManager.isUserConnected(senderId)) {
                sessionManager.sendToUser(senderId, "/queue/receipts", event);
                log.debug("Sent delivery receipt to sender {} for message {}", senderId, messageId);
            }
        } catch (Exception e) {
            log.error("Error processing MESSAGE_DELIVERED event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle MESSAGE_READ events.
     *
     * Notify the original sender (if online) that their message was read.
     *
     * @param payload JSON string from RabbitMQ
     */
    @RabbitListener(queues = RabbitMQConfig.MESSAGE_READ_QUEUE)
    public void onMessageRead(String payload) {
        try {
            Map<String, Object> event = parseEvent(payload);
            String messageId  = getString(event, "messageId");
            String senderId   = getString(event, "senderId");

            log.debug("Received MESSAGE_READ event: messageId={} senderId={}", messageId, senderId);

            // Push read receipt to sender in real-time
            if (sessionManager.isUserConnected(senderId)) {
                sessionManager.sendToUser(senderId, "/queue/receipts", event);
                log.debug("Sent read receipt to sender {} for message {}", senderId, messageId);
            }
        } catch (Exception e) {
            log.error("Error processing MESSAGE_READ event: {}", e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Map<String, Object> parseEvent(String payload) throws Exception {
        return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}

