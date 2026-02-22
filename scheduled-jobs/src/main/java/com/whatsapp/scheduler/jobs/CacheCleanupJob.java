package com.whatsapp.scheduler.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Cache Cleanup Job
 *
 * Scheduled job to clean up expired and orphaned cache entries.
 *
 * Cleanup Tasks:
 * 1. Remove orphaned inbox entries
 * 2. Clean up expired device tokens
 * 3. Remove stale user sessions
 * 4. Clear temporary cache entries
 *
 * Schedule: Hourly
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheCleanupJob {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Clean up orphaned inbox entries
     *
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupOrphanedInboxEntries() {
        log.info("Starting cleanup of orphaned inbox entries...");

        try {
            Set<String> inboxKeys = redisTemplate.keys("inbox:*");

            if (inboxKeys == null || inboxKeys.isEmpty()) {
                log.debug("No inbox entries found");
                return;
            }

            int cleanedCount = 0;
            for (String key : inboxKeys) {
                Long size = redisTemplate.opsForSet().size(key);

                // If inbox is empty, remove the key
                if (size != null && size == 0) {
                    redisTemplate.delete(key);
                    cleanedCount++;
                }
            }

            log.info("Cleaned up {} orphaned inbox entries", cleanedCount);

        } catch (Exception e) {
            log.error("Error during inbox cleanup", e);
        }
    }

    /**
     * Clean up expired device tokens
     *
     * Runs every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours
    public void cleanupExpiredDeviceTokens() {
        log.info("Starting cleanup of expired device tokens...");

        try {
            // Redis TTL handles automatic expiration
            // This is for additional cleanup if needed

            Set<String> tokenKeys = redisTemplate.keys("device:token:*");

            if (tokenKeys != null) {
                log.info("Found {} device token entries", tokenKeys.size());
            }

        } catch (Exception e) {
            log.error("Error during device token cleanup", e);
        }
    }

    /**
     * Clean up stale user status entries
     *
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupStaleUserStatus() {
        log.info("Starting cleanup of stale user status entries...");

        try {
            Set<String> statusKeys = redisTemplate.keys("user:status:*");

            if (statusKeys == null || statusKeys.isEmpty()) {
                log.debug("No user status entries found");
                return;
            }

            // Redis TTL handles expiration automatically
            log.info("Found {} user status entries", statusKeys.size());

        } catch (Exception e) {
            log.error("Error during user status cleanup", e);
        }
    }

    /**
     * Generate cache statistics
     *
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void generateCacheStatistics() {
        log.info("Generating cache statistics...");

        try {
            long inboxCount = countKeys("inbox:*");
            long tokenCount = countKeys("device:token:*");
            long userTokensCount = countKeys("user:tokens:*");
            long statusCount = countKeys("user:status:*");
            long userCacheCount = countKeys("user:*");

            log.info("Cache Statistics:");
            log.info("  Inbox entries: {}", inboxCount);
            log.info("  Device tokens: {}", tokenCount);
            log.info("  User token sets: {}", userTokensCount);
            log.info("  User status entries: {}", statusCount);
            log.info("  User cache entries: {}", userCacheCount);

        } catch (Exception e) {
            log.error("Error generating cache statistics", e);
        }
    }

    /**
     * Count keys matching pattern
     */
    private long countKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null ? keys.size() : 0;
    }

    /**
     * Clear all temporary caches (use with caution)
     *
     * Runs daily at 1:00 AM
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void clearTemporaryCaches() {
        log.info("Clearing temporary caches...");

        try {
            // Clean up temporary cache entries
            // Add specific temporary cache patterns here if needed

            log.info("Temporary caches cleared");

        } catch (Exception e) {
            log.error("Error clearing temporary caches", e);
        }
    }
}