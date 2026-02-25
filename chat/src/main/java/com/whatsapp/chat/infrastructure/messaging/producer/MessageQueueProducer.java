package com.whatsapp.chat.infrastructure.messaging.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.chat.domain.model.Message;
import com.whatsapp.chat.infrastructure.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Message Queue Producer
 *
 * Low-level component responsible for serialising domain events
 * and publishing them to the correct RabbitMQ routing key.
 *
 * This class is intentionally kept "dumb" – it handles only
 * serialisation and transport; business logic lives in the
 * application/domain layers.
 *
 * Routing key mapping:
 *   Message sent      → {@value RabbitMQConfig#MESSAGE_SENT_ROUTING_KEY}
 *   Message delivered → {@value RabbitMQConfig#MESSAGE_DELIVERED_ROUTING_KEY}
 *   Message read      → {@value RabbitMQConfig#MESSAGE_READ_ROUTING_KEY}
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageQueueProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper   objectMapper;

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Publish a MESSAGE_SENT event.
     *
     * @param message the domain message that was just persisted
     */
    public void publishMessageSent(Message message) {
        publish(message, "MESSAGE_SENT", RabbitMQConfig.MESSAGE_SENT_ROUTING_KEY);
    }

    /**
     * Publish a MESSAGE_DELIVERED event.
     *
     * @param message the domain message whose status changed to DELIVERED
     */
    public void publishMessageDelivered(Message message) {
        publish(message, "MESSAGE_DELIVERED", RabbitMQConfig.MESSAGE_DELIVERED_ROUTING_KEY);
    }

    /**
     * Publish a MESSAGE_READ event.
     *
     * @param message the domain message whose status changed to READ
     */
    public void publishMessageRead(Message message) {
        publish(message, "MESSAGE_READ", RabbitMQConfig.MESSAGE_READ_ROUTING_KEY);
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private void publish(Message message, String eventType, String routingKey) {
        try {
            String payload = objectMapper.writeValueAsString(buildPayload(message, eventType));
            rabbitTemplate.convertAndSend(RabbitMQConfig.MESSAGE_EXCHANGE, routingKey, payload);
            log.info("Published {} event for messageId={}", eventType, message.getId());
        } catch (JsonProcessingException e) {
            log.error("Serialisation error publishing {} event for messageId={}: {}",
                    eventType, message.getId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to publish {} event for messageId={}: {}",
                    eventType, message.getId(), e.getMessage(), e);
        }
    }

    private Map<String, Object> buildPayload(Message message, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType",      eventType);
        payload.put("messageId",      message.getId().getValue());
        payload.put("conversationId", message.getConversationId().getValue());
        payload.put("senderId",       message.getSenderId());
        payload.put("receiverId",     message.getReceiverId());
        payload.put("status",         message.getStatus().name());
        payload.put("contentType",    message.getContent().getType().name());
        payload.put("sentAt",         message.getSentAt() != null ? message.getSentAt().toString() : null);
        payload.put("timestamp",      Instant.now().toString());
        return payload;
    }
}

