package com.whatsapp.notification.service;

import com.google.firebase.messaging.*;
import com.whatsapp.notification.domain.model.PushNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Firebase Cloud Messaging Service
 *
 * Sends push notifications via Firebase Cloud Messaging (FCM).
 * Supports Android, iOS, and Web platforms.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FCMService {

    private final FirebaseMessaging firebaseMessaging;

    /**
     * Send notification to device token
     *
     * @param token Device token
     * @param notification Notification to send
     * @return Message ID if successful, null if failed
     */
    public String sendNotification(String token, PushNotification notification) {
        try {
            Message message = buildMessage(token, notification);

            String response = firebaseMessaging.send(message);

            log.info("Successfully sent FCM notification: {}", response);
            return response;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification to token: {}", token, e);
            handleFCMException(e, token);
            return null;
        }
    }

    /**
     * Send notification to multiple tokens (batch)
     *
     * @param tokens List of device tokens
     * @param notification Notification to send
     * @return Batch response
     */
    public BatchResponse sendBatchNotification(Iterable<String> tokens, PushNotification notification) {
        try {
            MulticastMessage message = buildMulticastMessage(tokens, notification);

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);

            log.info("Batch notification sent: {} success, {} failure",
                    response.getSuccessCount(),
                    response.getFailureCount());

            return response;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send batch FCM notification", e);
            return null;
        }
    }

    /**
     * Send notification to topic
     *
     * @param topic Topic name
     * @param notification Notification to send
     * @return Message ID if successful
     */
    public String sendToTopic(String topic, PushNotification notification) {
        try {
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(buildNotification(notification))
                    .putAllData(notification.getData())
                    .build();

            String response = firebaseMessaging.send(message);

            log.info("Successfully sent notification to topic {}: {}", topic, response);
            return response;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to topic: {}", topic, e);
            return null;
        }
    }

    /**
     * Build FCM message
     */
    private Message buildMessage(String token, PushNotification notification) {
        Message.Builder builder = Message.builder()
                .setToken(token)
                .setNotification(buildNotification(notification));

        // Add data payload
        if (notification.getData() != null && !notification.getData().isEmpty()) {
            builder.putAllData(notification.getData());
        }

        // Set Android config
        builder.setAndroidConfig(buildAndroidConfig(notification));

        // Set APNS config
        builder.setApnsConfig(buildApnsConfig(notification));

        // Set Web config
        builder.setWebpushConfig(buildWebpushConfig(notification));

        return builder.build();
    }

    /**
     * Build multicast message for batch sending
     */
    private MulticastMessage buildMulticastMessage(Iterable<String> tokens, PushNotification notification) {
        MulticastMessage.Builder builder = MulticastMessage.builder()
                .addAllTokens((Iterable<String>) tokens)
                .setNotification(buildNotification(notification));

        // Add data payload
        if (notification.getData() != null) {
            builder.putAllData(notification.getData());
        }

        return builder.build();
    }

    /**
     * Build notification payload
     */
    private Notification buildNotification(PushNotification notification) {
        return Notification.builder()
                .setTitle(notification.getTitle())
                .setBody(notification.getBody())
                .build();
    }

    /**
     * Build Android-specific configuration
     */
    private AndroidConfig buildAndroidConfig(PushNotification notification) {
        AndroidNotification.Priority priority = notification.getPriority() == PushNotification.Priority.HIGH
                ? AndroidNotification.Priority.HIGH
                : AndroidNotification.Priority.DEFAULT;

        return AndroidConfig.builder()
                .setNotification(AndroidNotification.builder()
                        .setTitle(notification.getTitle())
                        .setBody(notification.getBody())
                        .setPriority(priority)
                        .setSound("default")
                        .build())
                .setTtl(notification.getTtl() != null ? notification.getTtl() * 1000L : 3600000L) // Convert to ms
                .build();
    }

    /**
     * Build APNS-specific configuration
     */
    private ApnsConfig buildApnsConfig(PushNotification notification) {
        return ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setAlert(ApsAlert.builder()
                                .setTitle(notification.getTitle())
                                .setBody(notification.getBody())
                                .build())
                        .setSound("default")
                        .build())
                .build();
    }

    /**
     * Build Web Push configuration
     */
    private WebpushConfig buildWebpushConfig(PushNotification notification) {
        return WebpushConfig.builder()
                .setNotification(WebpushNotification.builder()
                        .setTitle(notification.getTitle())
                        .setBody(notification.getBody())
                        .build())
                .build();
    }

    /**
     * Handle FCM exceptions
     */
    private void handleFCMException(FirebaseMessagingException e, String token) {
        String errorCode = e.getErrorCode();

        switch (errorCode) {
            case "INVALID_ARGUMENT" ->
                    log.warn("Invalid token format: {}", token);
            case "REGISTRATION_TOKEN_NOT_REGISTERED" ->
                    log.warn("Token not registered (possibly uninstalled app): {}", token);
            case "SENDER_ID_MISMATCH" ->
                    log.warn("Token belongs to different sender: {}", token);
            case "QUOTA_EXCEEDED" ->
                    log.error("FCM quota exceeded");
            case "UNAVAILABLE" ->
                    log.error("FCM service unavailable");
            default ->
                    log.error("FCM error: {}", errorCode);
        }
    }
}