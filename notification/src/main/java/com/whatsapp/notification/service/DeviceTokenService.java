package com.whatsapp.notification.service;

import com.whatsapp.notification.domain.model.DeviceToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Device Token Service
 *
 * Manages device tokens for push notifications.
 * Stores tokens in Redis for fast access.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private static final String TOKEN_PREFIX = "device:token:";
    private static final String USER_TOKENS_PREFIX = "user:tokens:";
    private static final Duration TOKEN_TTL = Duration.ofDays(90); // 90 days

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Register device token for user
     *
     * @param userId User ID
     * @param token Device token
     * @param platform Device platform
     */
    public void registerToken(String userId, String token, DeviceToken.DevicePlatform platform) {
        log.info("Registering device token for user: {}, platform: {}", userId, platform);

        try {
            // Store token with user ID mapping
            String tokenKey = getTokenKey(token);
            String userTokensKey = getUserTokensKey(userId);

            // Store token → userId mapping
            redisTemplate.opsForValue().set(tokenKey, userId, TOKEN_TTL);

            // Add token to user's token set
            redisTemplate.opsForSet().add(userTokensKey, token);
            redisTemplate.expire(userTokensKey, TOKEN_TTL);

            log.info("Device token registered successfully for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to register device token for user: {}", userId, e);
        }
    }

    /**
     * Get all tokens for user
     *
     * @param userId User ID
     * @return Set of device tokens
     */
    public Set<String> getTokensForUser(String userId) {
        try {
            String userTokensKey = getUserTokensKey(userId);
            Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

            log.debug("Found {} tokens for user: {}", tokens != null ? tokens.size() : 0, userId);

            return tokens != null ? tokens : new HashSet<>();

        } catch (Exception e) {
            log.error("Failed to get tokens for user: {}", userId, e);
            return new HashSet<>();
        }
    }

    /**
     * Remove device token
     *
     * @param token Device token
     */
    public void removeToken(String token) {
        log.info("Removing device token: {}", token);

        try {
            String tokenKey = getTokenKey(token);

            // Get user ID associated with token
            String userId = redisTemplate.opsForValue().get(tokenKey);

            if (userId != null) {
                // Remove token from user's token set
                String userTokensKey = getUserTokensKey(userId);
                redisTemplate.opsForSet().remove(userTokensKey, token);
            }

            // Remove token mapping
            redisTemplate.delete(tokenKey);

            log.info("Device token removed successfully");

        } catch (Exception e) {
            log.error("Failed to remove device token", e);
        }
    }

    /**
     * Remove all tokens for user
     *
     * @param userId User ID
     */
    public void removeAllTokensForUser(String userId) {
        log.info("Removing all tokens for user: {}", userId);

        try {
            // Get all tokens
            Set<String> tokens = getTokensForUser(userId);

            // Remove each token
            for (String token : tokens) {
                String tokenKey = getTokenKey(token);
                redisTemplate.delete(tokenKey);
            }

            // Remove user's token set
            String userTokensKey = getUserTokensKey(userId);
            redisTemplate.delete(userTokensKey);

            log.info("All tokens removed for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to remove all tokens for user: {}", userId, e);
        }
    }

    /**
     * Check if token exists
     *
     * @param token Device token
     * @return true if exists
     */
    public boolean tokenExists(String token) {
        String tokenKey = getTokenKey(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
    }

    /**
     * Update token last used timestamp
     *
     * @param token Device token
     */
    public void updateTokenLastUsed(String token) {
        try {
            String tokenKey = getTokenKey(token);

            // Refresh TTL
            redisTemplate.expire(tokenKey, TOKEN_TTL);

            log.debug("Updated last used timestamp for token");

        } catch (Exception e) {
            log.error("Failed to update token last used", e);
        }
    }

    /**
     * Get token cache key
     */
    private String getTokenKey(String token) {
        return TOKEN_PREFIX + token;
    }

    /**
     * Get user tokens cache key
     */
    private String getUserTokensKey(String userId) {
        return USER_TOKENS_PREFIX + userId;
    }
}