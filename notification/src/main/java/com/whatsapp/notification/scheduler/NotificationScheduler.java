package com.whatsapp.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Notification Scheduler
 *
 * Scheduled tasks for notification service maintenance.
 *
 * Tasks:
 * - Clean up expired device tokens
 * - Generate usage statistics
 * - Health monitoring
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Clean up expired device tokens
     *
     * Runs every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired device tokens...");

        try {
            // Redis TTL handles expiration automatically
            // This is a placeholder for additional cleanup logic if needed

            long tokenCount = countDeviceTokens();
            log.info("Current device tokens in Redis: {}", tokenCount);

        } catch (Exception e) {
            log.error("Error during token cleanup", e);
        }
    }

    /**
     * Generate usage statistics
     *
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void generateStatistics() {
        log.debug("Generating notification statistics...");

        try {
            long tokenCount = countDeviceTokens();
            long userCount = countUsers();

            log.info("Statistics - Total tokens: {}, Total users: {}", tokenCount, userCount);

        } catch (Exception e) {
            log.error("Error generating statistics", e);
        }
    }

    /**
     * Health check for Firebase connection
     *
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void healthCheck() {
        log.debug("Running health check...");

        try {
            // Check Redis connection
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.debug("Health check passed");

        } catch (Exception e) {
            log.error("Health check failed", e);
        }
    }

    /**
     * Count total device tokens
     */
    private long countDeviceTokens() {
        Set<String> keys = redisTemplate.keys("device:token:*");
        return keys != null ? keys.size() : 0;
    }

    /**
     * Count total users with tokens
     */
    private long countUsers() {
        Set<String> keys = redisTemplate.keys("user:tokens:*");
        return keys != null ? keys.size() : 0;
    }
}