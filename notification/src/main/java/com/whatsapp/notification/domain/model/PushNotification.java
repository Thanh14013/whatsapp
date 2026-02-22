package com.whatsapp.notification.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Push Notification Domain Model
 *
 * Represents a push notification to be sent.
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotification {

    /**
     * Notification ID
     */
    private String id;

    /**
     * Target user ID
     */
    private String userId;

    /**
     * Notification type
     */
    private NotificationType type;

    /**
     * Notification title
     */
    private String title;

    /**
     * Notification body
     */
    private String body;

    /**
     * Additional data payload
     */
    private Map<String, String> data;

    /**
     * Priority (HIGH, NORMAL)
     */
    private Priority priority;

    /**
     * Time to live (seconds)
     */
    private Integer ttl;

    /**
     * Created timestamp
     */
    private Instant createdAt;

    /**
     * Sent timestamp
     */
    private Instant sentAt;

    /**
     * Delivery status
     */
    private DeliveryStatus status;

    /**
     * Notification Type Enum
     */
    public enum NotificationType {
        MESSAGE,
        TYPING,
        CALL,
        SYSTEM
    }

    /**
     * Priority Enum
     */
    public enum Priority {
        HIGH,
        NORMAL
    }

    /**
     * Delivery Status Enum
     */
    public enum DeliveryStatus {
        PENDING,
        SENT,
        DELIVERED,
        FAILED
    }
}