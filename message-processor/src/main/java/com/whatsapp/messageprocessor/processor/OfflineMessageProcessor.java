package com.whatsapp.messageprocessor.processor;

import com.whatsapp.messageprocessor.service.InboxCacheService;
import com.whatsapp.messageprocessor.service.PushNotificationService;
import com.whatsapp.messageprocessor.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Offline Message Processor
 *
 * Background processor for handling offline message delivery.
 * Runs periodically to check for users coming online and deliver cached messages.
 *
 * Processing Logic:
 * 1. Scan inbox cache for users with pending messages
 * 2. Check if users are now online
 * 3. Trigger message delivery for online users
 * 4. Clear inbox cache after successful delivery
 *
 * Note: This is a fallback mechanism. Primary delivery happens via WebSocket.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfflineMessageProcessor {

    private final UserStatusService userStatusService;
    private final InboxCacheService inboxCacheService;
    private final PushNotificationService pushNotificationService;

    /**
     * Process offline messages periodically
     *
     * Runs every 30 seconds to check for users coming online.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000) // 30 seconds
    public void processOfflineMessages() {
        log.trace("Running offline message processor...");

        try {
            // In a real implementation, you would:
            // 1. Scan Redis for all inbox keys
            // 2. Check if users are online
            // 3. Deliver messages to online users
            // 4. Clear inbox cache

            // This is a simplified implementation
            // In production, use Redis SCAN to iterate over inbox keys

            log.trace("Offline message processing completed");

        } catch (Exception e) {
            log.error("Error processing offline messages", e);
        }
    }

    /**
     * Process messages for specific user
     *
     * Called when a user comes online (via WebSocket connection event).
     *
     * @param userId User ID
     */
    public void processMessagesForUser(String userId) {
        log.info("Processing cached messages for user: {}", userId);

        try {
            // Check if user has messages in inbox
            if (!inboxCacheService.hasMessages(userId)) {
                log.debug("No cached messages for user: {}", userId);
                return;
            }

            // Get all cached messages
            Set<String> messageIds = inboxCacheService.getInboxMessages(userId);

            log.info("Found {} cached messages for user: {}", messageIds.size(), userId);

            // In a real implementation, you would:
            // 1. Fetch message details from MongoDB
            // 2. Send via WebSocket
            // 3. Clear from inbox cache

            // For now, just clear the inbox
            inboxCacheService.clearInbox(userId);

            log.info("Cached messages processed for user: {}", userId);

        } catch (Exception e) {
            log.error("Error processing messages for user: {}", userId, e);
        }
    }

    /**
     * Retry failed message delivery
     *
     * @param userId User ID
     * @param messageId Message ID
     */
    public void retryMessageDelivery(String userId, String messageId) {
        log.info("Retrying message delivery: {} to {}", messageId, userId);

        try {
            // Check if user is online
            boolean isOnline = userStatusService.isUserOnline(userId);

            if (isOnline) {
                log.info("User is online, attempting delivery: {}", userId);
                // WebSocket delivery would be triggered here
                // Remove from inbox after successful delivery
                inboxCacheService.removeFromInbox(userId, messageId);

            } else {
                log.debug("User is still offline: {}", userId);
                // Keep in inbox and send push notification
                pushNotificationService.sendMessageNotification(userId, "system", "Message preview");
            }

        } catch (Exception e) {
            log.error("Error retrying message delivery: {}", messageId, e);
        }
    }

    /**
     * Clean up expired messages from inbox
     *
     * Runs daily to remove old cached messages.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupExpiredMessages() {
        log.info("Running inbox cleanup...");

        try {
            // In production, scan Redis for expired inbox entries
            // and remove them to free up memory

            log.info("Inbox cleanup completed");

        } catch (Exception e) {
            log.error("Error during inbox cleanup", e);
        }
    }
}
