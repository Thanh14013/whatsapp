package com.whatsapp.messageprocessor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.messageprocessor.processor.MessageDeliveryProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Message Consumer
 *
 * Consumes messages from RabbitMQ queues and processes them.
 *
 * Queues:
 * - message.sent: New messages that need to be delivered
 * - message.delivered: Messages that were delivered (for status update)
 * - message.read: Messages that were read (for status update)
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final MessageDeliveryProcessor deliveryProcessor;
    private final ObjectMapper objectMapper;

    /**
     * Consume message.sent events
     *
     * Triggered when a new message is sent.
     * Process message delivery to receiver.
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
            String content = (String) event.get("content");

            log.info("Processing message delivery: {} from {} to {}", messageId, senderId, receiverId);

            // Process message delivery
            deliveryProcessor.processMessageDelivery(messageId, senderId, receiverId, content);

            log.info("Message delivery processed successfully: {}", messageId);

        } catch (Exception e) {
            log.error("Error processing message.sent event", e);
            // Message will be requeued by RabbitMQ based on retry configuration
        }
    }

    /**
     * Consume message.delivered events
     *
     * Triggered when a message is delivered to receiver's device.
     * Update delivery status in database.
     */
    @RabbitListener(queues = "${app.rabbitmq.queues.message-delivered:message.delivered}")
    public void handleMessageDelivered(String messageJson) {
        log.debug("Received message.delivered event");

        try {
            Map<String, Object> event = objectMapper.readValue(messageJson, Map.class);

            String messageId = (String) event.get("messageId");
            String receiverId = (String) event.get("receiverId");

            log.info("Processing message delivered: {} to {}", messageId, receiverId);

            // Clear from inbox cache since it's now delivered
            deliveryProcessor.handleMessageDelivered(messageId, receiverId);

            log.info("Message delivered processed successfully: {}", messageId);

        } catch (Exception e) {
            log.error("Error processing message.delivered event", e);
        }
    }

    /**
     * Consume message.read events
     *
     * Triggered when a message is read by receiver.
     * Update read status and clear caches.
     */
    @RabbitListener(queues = "${app.rabbitmq.queues.message-read:message.read}")
    public void handleMessageRead(String messageJson) {
        log.debug("Received message.read event");

        try {
            Map<String, Object> event = objectMapper.readValue(messageJson, Map.class);

            String messageId = (String) event.get("messageId");
            String receiverId = (String) event.get("receiverId");

            log.debug("Processing message read: {} by {}", messageId, receiverId);

            // Clear all related caches
            deliveryProcessor.handleMessageRead(messageId, receiverId);

            log.debug("Message read processed successfully: {}", messageId);

        } catch (Exception e) {
            log.error("Error processing message.read event", e);
        }
    }
}