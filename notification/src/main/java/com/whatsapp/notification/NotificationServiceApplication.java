package com.whatsapp.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

/**
 * Notification Service Application
 *
 * Microservice responsible for sending push notifications to user devices.
 *
 * Key Responsibilities:
 * - Manage device tokens (FCM, APNS)
 * - Send push notifications via Firebase Cloud Messaging (Android/Web)
 * - Send push notifications via Apple Push Notification Service (iOS)
 * - Track notification delivery status
 * - Handle notification retry logic
 * - Provide notification metrics
 *
 * Architecture:
 * - Event-Driven: Consumes messages from RabbitMQ
 * - Multi-Platform: Supports Android (FCM), iOS (APNS), Web (FCM)
 * - Async Processing: Non-blocking notification sending
 * - Caching: Redis for device token storage
 *
 * Message Flow:
 * 1. Consume notification request from RabbitMQ (message.sent queue)
 * 2. Fetch device tokens for target user from Redis
 * 3. Send notification via appropriate service (FCM/APNS)
 * 4. Track delivery status
 * 5. Publish metrics
 *
 * @author WhatsApp Clone Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        log.info("Starting WhatsApp Clone Notification Service...");
        SpringApplication.run(NotificationServiceApplication.class, args);
        log.info("WhatsApp Clone Notification Service started successfully!");
    }
}
