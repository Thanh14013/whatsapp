package com.whatsapp.user.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.user.application.dto.UserDto;
import com.whatsapp.user.domain.model.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * User Cache Service
 *
 * Manages user data caching in Redis.
 * Improves read performance by reducing database queries.
 *
 * Cache Strategy:
 * - User data: 1 hour TTL
 * - User status: 5 minutes TTL
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private static final String USER_CACHE_PREFIX = "user:";
    private static final String USER_STATUS_PREFIX = "user:status:";
    private static final Duration USER_CACHE_TTL = Duration.ofHours(1);
    private static final Duration STATUS_CACHE_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Cache user data
     */
    public void cacheUser(UserDto user) {
        try {
            String key = getUserCacheKey(user.getId());
            String value = objectMapper.writeValueAsString(user);

            redisTemplate.opsForValue().set(key, value, USER_CACHE_TTL);

            log.debug("User cached: {}", user.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache user: {}", user.getId(), e);
        }
    }

    /**
     * Get cached user data
     */
    public UserDto getUser(String userId) {
        try {
            String key = getUserCacheKey(userId);
            String value = redisTemplate.opsForValue().get(key);

            if (value != null) {
                log.debug("Cache hit for user: {}", userId);
                return objectMapper.readValue(value, UserDto.class);
            }

            log.debug("Cache miss for user: {}", userId);
            return null;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached user: {}", userId, e);
            return null;
        }
    }

    /**
     * Evict user from cache
     */
    public void evictUser(String userId) {
        String key = getUserCacheKey(userId);
        redisTemplate.delete(key);

        log.debug("User evicted from cache: {}", userId);
    }

    /**
     * Cache user status
     */
    public void cacheUserStatus(String userId, UserStatus status) {
        String key = getUserStatusKey(userId);
        redisTemplate.opsForValue().set(key, status.name(), STATUS_CACHE_TTL);

        log.debug("User status cached: {} - {}", userId, status);
    }

    /**
     * Update user status in cache
     */
    public void updateUserStatus(String userId, UserStatus status) {
        // Update in user cache
        UserDto user = getUser(userId);
        if (user != null) {
            user.setStatus(status);
            cacheUser(user);
        }

        // Update status cache
        cacheUserStatus(userId, status);
    }

    /**
     * Get cached user status
     */
    public UserStatus getUserStatus(String userId) {
        String key = getUserStatusKey(userId);
        String value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            return UserStatus.valueOf(value);
        }

        return null;
    }

    /**
     * Cache multiple users (batch operation)
     */
    public void cacheUsers(Iterable<UserDto> users) {
        users.forEach(this::cacheUser);
    }

    /**
     * Clear all user caches (use with caution)
     */
    public void clearAllUserCaches() {
        var keys = redisTemplate.keys(USER_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared {} user cache entries", keys.size());
        }
    }

    /**
     * Get user cache key
     */
    private String getUserCacheKey(String userId) {
        return USER_CACHE_PREFIX + userId;
    }

    /**
     * Get user status cache key
     */
    private String getUserStatusKey(String userId) {
        return USER_STATUS_PREFIX + userId;
    }
}