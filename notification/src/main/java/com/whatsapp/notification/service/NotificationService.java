package com.whatsapp.notification.service;

import com.whatsapp.notification.domain.model.DeviceToken;
import com.whatsapp.notification.domain.model.PushNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Notification Service
 *
 * Main orchestration service for sending push notifications.
 * Coordinates between device token management and platform-specific services.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final DeviceTokenService deviceTokenService;
    private final FCMService fcmService;
    private final APNSNotificationService apnsNotificationService;
    private final NotificationMetrics notificationMetrics;

    /**
     * Send notification to user
     *
     * Fetches all device tokens for the user and sends notification to each device.
     *
     * @param userId Target user ID
     * @param title  Notification title
     * @param body   Notification body
     * @param data   Additional data payload
     */
    @Async
    public void sendNotification(String userId, String title, String body, Map<String, String> data) {
        log.info("Sending notification to user: {}", userId);

        try {
            // Get all device tokens for user
            Set<String> tokens = deviceTokenService.getTokensForUser(userId);

            if (tokens.isEmpty()) {
                log.warn("No device tokens found for user: {}", userId);
                notificationMetrics.recordNotificationSkipped();
                return;
            }

            log.debug("Found {} device tokens for user: {}", tokens.size(), userId);

            // Create notification object
            PushNotification notification = PushNotification.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .type(PushNotification.NotificationType.MESSAGE)
                    .title(title)
                    .body(body)
                    .data(data)
                    .priority(PushNotification.Priority.HIGH)
                    .ttl(86400) // 24 hours
                    .createdAt(Instant.now())
                    .status(PushNotification.DeliveryStatus.PENDING)
                    .build();

            // Send to all tokens
            int successCount = 0;
            for (String token : tokens) {
                boolean success = sendToToken(token, notification);
                if (success) {
                    successCount++;
                }
            }

            log.info("Notification sent: {} successful out of {} devices", successCount, tokens.size());

            // Record metrics
            notificationMetrics.recordNotificationSent();

        } catch (Exception e) {
            log.error("Failed to send notification to user: {}", userId, e);
            notificationMetrics.recordNotificationFailed();
        }
    }

    /**
     * Send notification to specific token
     *
     * @param token        Device token
     * @param notification Notification to send
     * @return true if successful
     */
    private boolean sendToToken(String token, PushNotification notification) {
        try {
            // For simplicity, assume all tokens use FCM
            // In production, you'd need to track platform per token
            String messageId = fcmService.sendNotification(token, notification);

            if (messageId != null) {
                log.debug("Notification sent to token: {}, messageId: {}", token, messageId);
                return true;
            } else {
                log.warn("Failed to send notification to token: {}", token);
                // Token might be invalid, consider removing it
                deviceTokenService.removeToken(token);
                return false;
            }

        } catch (Exception e) {
            log.error("Error sending notification to token: {}", token, e);
            return false;
        }
    }

    /**
     * Send message notification
     *
     * Specialized method for message notifications.
     *
     * @param userId         Receiver user ID
     * @param senderId       Sender user ID
     * @param senderName     Sender display name
     * @param messagePreview Message content preview
     */
    public void sendMessageNotification(String userId, String senderId, String senderName, String messagePreview) {
        log.info("Sending message notification: user={}, sender={}", userId, senderId);

        Map<String, String> data = Map.of(
                "type", "message",
                "senderId", senderId,
                "senderName", senderName);

        String title = "New message from " + senderName;
        String body = messagePreview;

        sendNotification(userId, title, body, data);
    }

    /**
     * Send typing notification
     *
     * @param userId   User to notify
     * @param senderId User who is typing
     */
    public void sendTypingNotification(String userId, String senderId) {
        log.debug("Sending typing notification: user={}, sender={}", userId, senderId);

        Map<String, String> data = Map.of(
                "type", "typing",
                "senderId", senderId);

        // Typing notifications are silent (data-only)
        sendNotification(userId, "", "", data);
    }

    /**
     * Send call notification
     *
     * @param userId     User to notify
     * @param callerId   Caller ID
     * @param callerName Caller name
     */
    public void sendCallNotification(String userId, String callerId, String callerName) {
        log.info("Sending call notification: user={}, caller={}", userId, callerId);

        Map<String, String> data = Map.of(
                "type", "call",
                "callerId", callerId,
                "callerName", callerName);

        String title = "Incoming call";
        String body = callerName + " is calling...";

        sendNotification(userId, title, body, data);
    }
}
