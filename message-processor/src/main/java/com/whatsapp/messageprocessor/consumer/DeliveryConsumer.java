package com.whatsapp.messageprocessor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.messageprocessor.service.DeliveryTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Delivery Consumer
 *
 * Consumes message delivery and read receipt events from RabbitMQ.
 * Updates message status in MongoDB accordingly.
 *
 * Queues:
 * - message.delivered: Updates status to DELIVERED
 * - message.read: Updates status to READ
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryConsumer {

    private final DeliveryTrackingService deliveryTrackingService;
    private final ObjectMapper objectMapper;

    /**
     * Consume message.delivered events
     *
     * Updates message status to DELIVERED in database.
     */
    @RabbitListener(queues = "${app.rabbitmq.queues.message-delivered:message.delivered}")
    public void handleMessageDelivered(String messageJson) {
        log.debug("Received message.delivered event");

        try {
            Map<String, Object> event = objectMapper.readValue(messageJson, Map.class);

            String eventType = (String) event.get("eventType");
            if (!"MESSAGE_DELIVERED".equals(eventType)) {
                log.warn("Unexpected event type: {}", eventType);
                return;
            }

            String messageId = (String) event.get("messageId");
            String receiverId = (String) event.get("receiverId");

            log.info("Processing message delivered: {} to {}", messageId, receiverId);

            // Update message status
            deliveryTrackingService.markAsDelivered(messageId);

            log.info("Message delivered status updated: {}", messageId);

        } catch (Exception e) {
            log.error("Error processing message.delivered event", e);
            // Message will be requeued by RabbitMQ based on retry configuration
        }
    }

    /**
     * Consume message.read events
     *
     * Updates message status to READ in database.
     */
    @RabbitListener(queues = "${app.rabbitmq.queues.message-read:message.read}")
    public void handleMessageRead(String messageJson) {
        log.debug("Received message.read event");

        try {
            Map<String, Object> event = objectMapper.readValue(messageJson, Map.class);

            String eventType = (String) event.get("eventType");
            if (!"MESSAGE_READ".equals(eventType)) {
                log.warn("Unexpected event type: {}", eventType);
                return;
            }

            String messageId = (String) event.get("messageId");
            String receiverId = (String) event.get("receiverId");

            log.info("Processing message read: {} by {}", messageId, receiverId);

            // Update message status
            deliveryTrackingService.markAsRead(messageId);

            log.info("Message read status updated: {}", messageId);

        } catch (Exception e) {
            log.error("Error processing message.read event", e);
            // Message will be requeued by RabbitMQ based on retry configuration
        }
    }
}
