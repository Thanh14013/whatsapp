package com.whatsapp.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Notification Consumer
 *
 * Consumes messages from RabbitMQ and sends push notifications.
 *
 * Queues:
 * - message.sent: Triggers notification when new message is sent
 * - user.status.changed: Triggers notification when user status changes
 * (optional)
 *
 * Event Processing:
 * 1. Receive event from RabbitMQ
 * 2. Parse event payload
 * 3. Extract notification data
 * 4. Invoke NotificationService to send notification
 * 5. Log result
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Consume message.sent events
     *
     * Triggered when a new message is sent.
     * Sends push notification to receiver.
     */
    @RabbitListener(queues = "${app.rabbitmq.queues.message-sent:message.sent}")
    public void handleMessageSent(String messageJson) {
        log.debug("Received message.sent event");

        try {
            Map<String, Object> event = objectMapper.readValue(messageJson, Map.class);

            String eventType = (String) event.get("eventType");
            if (!"MESSAGE_SENT".equals(eventType)) {
                log.warn("Unexpected event type: {}", eventType);
                return;
            }

            String messageId = (String) event.get("messageId");
            String senderId = (String) event.get("senderId");
            String receiverId = (String) event.get("receiverId");
            String senderName = (String) event.get("senderName");
            String content = (String) event.get("content");

            log.info("Processing message notification: {} from {} to {}", messageId, senderId, receiverId);

            // Truncate content for preview (max 100 characters)
            String preview = content != null && content.length() > 100
                    ? content.substring(0, 97) + "..."
                    : content;

            // Send notification
            notificationService.sendMessageNotification(receiverId, senderId, senderName, preview);

            log.info("Message notification sent successfully: {}", messageId);

        } catch (Exception e) {
            log.error("Error processing message.sent event", e);
            // Message will be requeued by RabbitMQ based on retry configuration
        }
    }

    /**
     * Consume user.status.changed events
     *
     * Triggered when user status changes (online/offline).
     * Can be used for presence notifications.
     */
    @RabbitListener(queues = "${app.rabbitmq.queues.user-status-changed:user.status.changed}")
    public void handleUserStatusChanged(String messageJson) {
        log.debug("Received user.status.changed event");

        try {
            Map<String, Object> event = objectMapper.readValue(messageJson, Map.class);

            String userId = (String) event.get("userId");
            String status = (String) event.get("status");

            log.debug("User status changed: {} -> {}", userId, status);

            // Optional: Send notifications to user's contacts about status change
            // For WhatsApp-like behavior, this is typically not done
            // But can be used for "last seen" updates

        } catch (Exception e) {
            log.error("Error processing user.status.changed event", e);
        }
    }

    /**
     * Consume typing events (optional)
     *
     * Can be used to send typing indicator notifications.
     */
    public void handleTypingEvent(String messageJson) {
        log.trace("Received typing event");

        try {
            Map<String, Object> event = objectMapper.readValue(messageJson, Map.class);

            String senderId = (String) event.get("senderId");
            String receiverId = (String) event.get("receiverId");
            boolean isTyping = Boolean.TRUE.equals(event.get("isTyping"));

            if (isTyping) {
                log.trace("User {} is typing to {}", senderId, receiverId);
                notificationService.sendTypingNotification(receiverId, senderId);
            }

        } catch (Exception e) {
            log.error("Error processing typing event", e);
        }
    }
}
