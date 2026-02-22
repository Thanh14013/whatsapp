package com.whatsapp.chat.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.chat.domain.model.Message;
import com.whatsapp.chat.infrastructure.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Message Event Publisher
 *
 * Publishes message-related events to RabbitMQ for other services to consume.
 *
 * Events:
 * - message.sent: When a new message is sent
 * - message.delivered: When message is delivered to recipient
 * - message.read: When message is read by recipient
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish message sent event
     */
    public void publishMessageSent(Message message) {
        try {
            Map<String, Object> event = createMessageEvent(message, "MESSAGE_SENT");
            String messageJson = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MESSAGE_EXCHANGE,
                    RabbitMQConfig.MESSAGE_SENT_ROUTING_KEY,
                    messageJson);

            log.info("Published MESSAGE_SENT event for message: {}", message.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish MESSAGE_SENT event for message: {}", message.getId(), e);
        }
    }

    /**
     * Publish message delivered event
     */
    public void publishMessageDelivered(Message message) {
        try {
            Map<String, Object> event = createMessageEvent(message, "MESSAGE_DELIVERED");
            String messageJson = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MESSAGE_EXCHANGE,
                    RabbitMQConfig.MESSAGE_DELIVERED_ROUTING_KEY,
                    messageJson);

            log.info("Published MESSAGE_DELIVERED event for message: {}", message.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish MESSAGE_DELIVERED event for message: {}", message.getId(), e);
        }
    }

    /**
     * Publish message read event
     */
    public void publishMessageRead(Message message) {
        try {
            Map<String, Object> event = createMessageEvent(message, "MESSAGE_READ");
            String messageJson = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MESSAGE_EXCHANGE,
                    RabbitMQConfig.MESSAGE_READ_ROUTING_KEY,
                    messageJson);

            log.info("Published MESSAGE_READ event for message: {}", message.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish MESSAGE_READ event for message: {}", message.getId(), e);
        }
    }

    /**
     * Create message event payload
     */
    private Map<String, Object> createMessageEvent(Message message, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("messageId", message.getId().getValue());
        event.put("conversationId", message.getConversationId().getValue());
        event.put("senderId", message.getSenderId());
        event.put("receiverId", message.getReceiverId());
        event.put("status", message.getStatus().name());
        event.put("contentType", message.getContent().getType().name());
        event.put("sentAt", message.getSentAt().toString());
        event.put("timestamp", Instant.now().toString());
        return event;
    }
}
