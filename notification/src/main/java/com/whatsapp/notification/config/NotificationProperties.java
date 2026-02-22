package com.whatsapp.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Notification Configuration Properties
 *
 * Binds application.yml notification settings to Java objects.
 *
 * @author WhatsApp Clone Team
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    private DefaultSettings defaultSettings = new DefaultSettings();
    private MessageSettings message = new MessageSettings();
    private TypingSettings typing = new TypingSettings();
    private SystemSettings system = new SystemSettings();

    @Data
    public static class DefaultSettings {
        private Integer ttl = 3600;
        private String priority = "NORMAL";
    }

    @Data
    public static class MessageSettings {
        private Integer ttl = 3600;
        private String priority = "HIGH";
        private String sound = "default";
        private Boolean badge = true;
    }

    @Data
    public static class TypingSettings {
        private Integer ttl = 10;
        private String priority = "NORMAL";
        private String sound = "none";
    }

    @Data
    public static class SystemSettings {
        private Integer ttl = 86400;
        private String priority = "NORMAL";
        private String sound = "default";
    }
}