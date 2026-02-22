package com.whatsapp.messageprocessor.processor;

import com.whatsapp.messageprocessor.service.InboxCacheService;
import com.whatsapp.messageprocessor.service.PushNotificationService;
import com.whatsapp.messageprocessor.service.UserStatusService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Message Delivery Processor
 *
 * Processes message delivery logic.
 * Determines if user is online/offline and handles accordingly.
 *
 * Delivery Strategy:
 * 1. Check if receiver is online (via Redis cache)
 * 2. If online: WebSocket delivery (handled by Chat Service)
 * 3. If offline:
 *    - Add to inbox cache for quick delivery when user comes online
 *    - Send push notification
 * 4. Update message status
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDeliveryProcessor {

    private final UserStatusService userStatusService;
    private final InboxCacheService inboxCacheService;
    private final PushNotificationService pushNotificationService;

    /**
     * Process message delivery
     *
     * Main logic for handling message delivery to receiver.
     */
    public void processMessageDelivery(String messageId, String senderId, String receiverId, String content) {
        log.debug("Processing message delivery: {} to {}", messageId, receiverId);

        try {
            // Check if receiver is online
            boolean isOnline = userStatusService.isUserOnline(receiverId);

            if (isOnline) {
                log.debug("Receiver is online: {}. WebSocket delivery will be handled by Chat Service", receiverId);
                // WebSocket delivery is handled by Chat Service in real-time
                // This processor doesn't need to do anything for online users

            } else {
                log.info("Receiver is offline: {}. Caching message and sending push notification", receiverId);

                // Add message to inbox cache for quick delivery when user comes online
                inboxCacheService.addToInbox(receiverId, messageId);

                // Send push notification
                sendPushNotification(receiverId, senderId, content);
            }

        } catch (Exception e) {
            log.error("Error processing message delivery: {}", messageId, e);
            throw new RuntimeException("Failed to process message delivery", e);
        }
    }

    /**
     * Handle message delivered event
     *
     * Called when message is successfully delivered to receiver's device.
     */
    public void handleMessageDelivered(String messageId, String receiverId) {
        log.debug("Handling message delivered: {} to {}", messageId, receiverId);

        try {
            // Remove from inbox cache since it's now delivered
            inboxCacheService.removeFromInbox(receiverId, messageId);

            log.debug("Message removed from inbox cache: {}", messageId);

        } catch (Exception e) {
            log.error("Error handling message delivered: {}", messageId, e);
        }
    }

    /**
     * Handle message read event
     *
     * Called when message is read by receiver.
     */
    public void handleMessageRead(String messageId, String receiverId) {
        log.debug("Handling message read: {} by {}", messageId, receiverId);

        try {
            // Clear all caches related to this message
            inboxCacheService.removeFromInbox(receiverId, messageId);

            log.debug("Caches cleared for read message: {}", messageId);

        } catch (Exception e) {
            log.error("Error handling message read: {}", messageId, e);
        }
    }

    /**
     * Send push notification to offline user
     */
    private void sendPushNotification(String receiverId, String senderId, String content) {
        try {
            log.debug("Sending push notification to user: {}", receiverId);

            pushNotificationService.sendMessageNotification(
                    receiverId,
                    senderId,
                    truncateContent(content)
            );

            log.debug("Push notification sent successfully to user: {}", receiverId);

        } catch (Exception e) {
            log.error("Failed to send push notification to user: {}", receiverId, e);
            // Don't throw exception - push notification failure shouldn't break message delivery
        }
    }

    /**
     * Truncate content for notification preview
     */
    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 100 ? content.substring(0, 97) + "..." : content;
    }
}