package com.whatsapp.messageprocessor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Inbox Cache Service
 *
 * Manages inbox cache for offline messages.
 * Messages are cached in Redis when users are offline,
 * and delivered when they come back online.
 *
 * Cache Structure:
 * - Key: inbox:{userId}
 * - Value: Set of message IDs
 * - TTL: 7 days
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxCacheService {

    private static final String INBOX_PREFIX = "inbox:";
    private static final Duration INBOX_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Add message to user's inbox
     *
     * @param userId User ID
     * @param messageId Message ID
     */
    public void addToInbox(String userId, String messageId) {
        log.debug("Adding message to inbox: user={}, message={}", userId, messageId);

        try {
            String key = getInboxKey(userId);

            // Add message to set
            redisTemplate.opsForSet().add(key, messageId);

            // Set TTL
            redisTemplate.expire(key, INBOX_TTL);

            log.debug("Message added to inbox successfully: {}", messageId);

        } catch (Exception e) {
            log.error("Error adding message to inbox: user={}, message={}", userId, messageId, e);
        }
    }

    /**
     * Remove message from user's inbox
     *
     * @param userId User ID
     * @param messageId Message ID
     */
    public void removeFromInbox(String userId, String messageId) {
        log.debug("Removing message from inbox: user={}, message={}", userId, messageId);

        try {
            String key = getInboxKey(userId);
            redisTemplate.opsForSet().remove(key, messageId);

            log.debug("Message removed from inbox successfully: {}", messageId);

        } catch (Exception e) {
            log.error("Error removing message from inbox: user={}, message={}", userId, messageId, e);
        }
    }

    /**
     * Get all messages in user's inbox
     *
     * @param userId User ID
     * @return Set of message IDs
     */
    public Set<String> getInboxMessages(String userId) {
        log.debug("Getting inbox messages for user: {}", userId);

        try {
            String key = getInboxKey(userId);
            Set<String> messages = redisTemplate.opsForSet().members(key);

            log.debug("Found {} messages in inbox for user: {}", messages != null ? messages.size() : 0, userId);

            return messages;

        } catch (Exception e) {
            log.error("Error getting inbox messages for user: {}", userId, e);
            return Set.of();
        }
    }

    /**
     * Clear user's inbox
     *
     * @param userId User ID
     */
    public void clearInbox(String userId) {
        log.debug("Clearing inbox for user: {}", userId);

        try {
            String key = getInboxKey(userId);
            redisTemplate.delete(key);

            log.debug("Inbox cleared successfully for user: {}", userId);

        } catch (Exception e) {
            log.error("Error clearing inbox for user: {}", userId, e);
        }
    }

    /**
     * Check if user has messages in inbox
     *
     * @param userId User ID
     * @return true if inbox is not empty
     */
    public boolean hasMessages(String userId) {
        try {
            String key = getInboxKey(userId);
            Long size = redisTemplate.opsForSet().size(key);

            return size != null && size > 0;

        } catch (Exception e) {
            log.error("Error checking inbox for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Get inbox size
     *
     * @param userId User ID
     * @return Number of messages in inbox
     */
    public long getInboxSize(String userId) {
        try {
            String key = getInboxKey(userId);
            Long size = redisTemplate.opsForSet().size(key);

            return size != null ? size : 0;

        } catch (Exception e) {
            log.error("Error getting inbox size for user: {}", userId, e);
            return 0;
        }
    }

    /**
     * Get inbox key for user
     */
    private String getInboxKey(String userId) {
        return INBOX_PREFIX + userId;
    }
}
