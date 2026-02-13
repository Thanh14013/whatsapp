package com.whatsapp.notification.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Device Token Domain Model
 *
 * Represents a device token for push notifications.
 * Each user can have multiple device tokens (multiple devices).
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceToken {

    /**
     * User ID
     */
    private String userId;

    /**
     * Device token (FCM token or APNS token)
     */
    private String token;

    /**
     * Device platform (ANDROID, IOS, WEB)
     */
    private DevicePlatform platform;

    /**
     * Device name/model (optional)
     */
    private String deviceName;

    /**
     * Token registration timestamp
     */
    private Instant registeredAt;

    /**
     * Last notification sent timestamp
     */
    private Instant lastUsedAt;

    /**
     * Is token active
     */
    private boolean active;

    /**
     * Device Platform Enum
     */
    public enum DevicePlatform {
        ANDROID,
        IOS,
        WEB
    }
}