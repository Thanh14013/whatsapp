package com.whatsapp.messageprocessor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * User Status Service
 *
 * Manages user online/offline status using Redis.
 * Used to determine if users are online for message delivery.
 *
 * Status Tracking:
 * - Users marked as online when they connect (WebSocket/HTTP)
 * - Users marked as offline when they disconnect
 * - TTL of 5 minutes (auto-expire if no heartbeat)
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatusService {

    private static final String ONLINE_USERS_PREFIX = "user:online:";
    private static final Duration ONLINE_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Check if user is online
     *
     * @param userId User ID
     * @return true if user is online
     */
    public boolean isUserOnline(String userId) {
        try {
            String key = getOnlineKey(userId);
            Boolean exists = redisTemplate.hasKey(key);

            log.debug("User online status: {} -> {}", userId, exists);

            return Boolean.TRUE.equals(exists);

        } catch (Exception e) {
            log.error("Error checking user online status: {}", userId, e);
            return false;
        }
    }

    /**
     * Mark user as online
     *
     * @param userId User ID
     */
    public void markUserOnline(String userId) {
        try {
            String key = getOnlineKey(userId);
            redisTemplate.opsForValue().set(key, "1", ONLINE_TTL);

            log.debug("User marked as online: {}", userId);

        } catch (Exception e) {
            log.error("Error marking user as online: {}", userId, e);
        }
    }

    /**
     * Mark user as offline
     *
     * @param userId User ID
     */
    public void markUserOffline(String userId) {
        try {
            String key = getOnlineKey(userId);
            redisTemplate.delete(key);

            log.debug("User marked as offline: {}", userId);

        } catch (Exception e) {
            log.error("Error marking user as offline: {}", userId, e);
        }
    }

    /**
     * Refresh user online status (heartbeat)
     *
     * @param userId User ID
     */
    public void refreshOnlineStatus(String userId) {
        try {
            String key = getOnlineKey(userId);
            redisTemplate.expire(key, ONLINE_TTL);

            log.trace("User online status refreshed: {}", userId);

        } catch (Exception e) {
            log.error("Error refreshing user online status: {}", userId, e);
        }
    }

    /**
     * Get online key for user
     */
    private String getOnlineKey(String userId) {
        return ONLINE_USERS_PREFIX + userId;
    }
}
