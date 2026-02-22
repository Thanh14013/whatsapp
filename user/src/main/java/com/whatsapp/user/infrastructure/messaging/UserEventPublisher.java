package com.whatsapp.user.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.user.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * User Event Publisher
 *
 * Publishes user-related events to RabbitMQ.
 * Other services can subscribe to these events.
 *
 * Events:
 * - user.created: When a new user registers
 * - user.updated: When user profile is updated
 * - user.deleted: When user account is deleted
 * - user.status.changed: When user status changes (online/offline)
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.rabbitmq.exchanges.user-events:user.events}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing-keys.user-created:user.created}")
    private String userCreatedRoutingKey;

    @Value("${app.rabbitmq.routing-keys.user-updated:user.updated}")
    private String userUpdatedRoutingKey;

    @Value("${app.rabbitmq.routing-keys.user-deleted:user.deleted}")
    private String userDeletedRoutingKey;

    @Value("${app.rabbitmq.routing-keys.user-status-changed:user.status.changed}")
    private String userStatusChangedRoutingKey;

    /**
     * Publish user created event
     */
    public void publishUserCreated(User user) {
        try {
            Map<String, Object> event = createUserEvent(user, "USER_CREATED");
            String message = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(exchangeName, userCreatedRoutingKey, message);

            log.info("Published USER_CREATED event for user: {}", user.getId().getValue());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish USER_CREATED event for user: {}", user.getId().getValue(), e);
        }
    }

    /**
     * Publish user updated event
     */
    public void publishUserUpdated(User user) {
        try {
            Map<String, Object> event = createUserEvent(user, "USER_UPDATED");
            String message = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(exchangeName, userUpdatedRoutingKey, message);

            log.info("Published USER_UPDATED event for user: {}", user.getId().getValue());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish USER_UPDATED event for user: {}", user.getId().getValue(), e);
        }
    }

    /**
     * Publish user deactivated event
     */
    public void publishUserDeactivated(User user) {
        try {
            Map<String, Object> event = createUserEvent(user, "USER_DEACTIVATED");
            String message = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(exchangeName, userDeletedRoutingKey, message);

            log.info("Published USER_DEACTIVATED event for user: {}", user.getId().getValue());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish USER_DEACTIVATED event for user: {}", user.getId().getValue(), e);
        }
    }

    /**
     * Publish user status changed event
     */
    public void publishUserStatusChanged(User user) {
        try {
            Map<String, Object> event = createStatusChangeEvent(user);
            String message = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(exchangeName, userStatusChangedRoutingKey, message);

            log.debug("Published USER_STATUS_CHANGED event for user: {} - {}",
                    user.getId().getValue(), user.getStatus());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish USER_STATUS_CHANGED event for user: {}",
                    user.getId().getValue(), e);
        }
    }

    /**
     * Create user event payload
     */
    private Map<String, Object> createUserEvent(User user, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("userId", user.getId().getValue());
        event.put("username", user.getUsername());
        event.put("email", user.getEmail().getValue());
        event.put("phoneNumber", user.getPhoneNumber().getValue());
        event.put("displayName", user.getProfile().getDisplayName());
        event.put("status", user.getStatus().name());
        event.put("active", user.isActive());

        return event;
    }

    /**
     * Create status change event payload
     */
    private Map<String, Object> createStatusChangeEvent(User user) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_STATUS_CHANGED");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("userId", user.getId().getValue());
        event.put("status", user.getStatus().name());
        event.put("online", user.isOnline());

        return event;
    }
}