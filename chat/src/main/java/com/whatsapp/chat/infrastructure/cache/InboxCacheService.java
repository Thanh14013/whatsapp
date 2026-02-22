package com.whatsapp.chat.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Inbox Cache Service
 *
 * Manages caching of conversation data and online users using Redis.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CONVERSATION_KEY_PREFIX = "conversation:";
    private static final String USER_CONVERSATIONS_KEY_PREFIX = "user:conversations:";
    private static final String ONLINE_USERS_KEY = "online:users";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /**
     * Cache conversation
     */
    public void cacheConversation(String conversationId, Object conversation) {
        try {
            String key = CONVERSATION_KEY_PREFIX + conversationId;
            redisTemplate.opsForValue().set(key, conversation, CACHE_TTL);
            log.debug("Cached conversation: {}", conversationId);
        } catch (Exception e) {
            log.error("Failed to cache conversation: {}", conversationId, e);
        }
    }

    /**
     * Get cached conversation
     */
    public Object getConversation(String conversationId) {
        try {
            String key = CONVERSATION_KEY_PREFIX + conversationId;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get cached conversation: {}", conversationId, e);
            return null;
        }
    }

    /**
     * Evict conversation from cache
     */
    public void evictConversation(String conversationId) {
        try {
            String key = CONVERSATION_KEY_PREFIX + conversationId;
            redisTemplate.delete(key);
            log.debug("Evicted conversation from cache: {}", conversationId);
        } catch (Exception e) {
            log.error("Failed to evict conversation: {}", conversationId, e);
        }
    }

    /**
     * Cache user conversations list
     */
    public void cacheUserConversations(String userId, Set<String> conversationIds) {
        try {
            String key = USER_CONVERSATIONS_KEY_PREFIX + userId;
            redisTemplate.opsForSet().add(key, conversationIds.toArray());
            redisTemplate.expire(key, CACHE_TTL);
            log.debug("Cached conversations for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to cache user conversations: {}", userId, e);
        }
    }

    /**
     * Get cached user conversations
     */
    public Set<Object> getUserConversations(String userId) {
        try {
            String key = USER_CONVERSATIONS_KEY_PREFIX + userId;
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Failed to get cached user conversations: {}", userId, e);
            return null;
        }
    }

    /**
     * Add user to online users set
     */
    public void addOnlineUser(String userId) {
        try {
            redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
            log.debug("Added online user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to add online user: {}", userId, e);
        }
    }

    /**
     * Remove user from online users set
     */
    public void removeOnlineUser(String userId) {
        try {
            redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
            log.debug("Removed online user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to remove online user: {}", userId, e);
        }
    }

    /**
     * Check if user is online
     */
    public boolean isUserOnline(String userId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId));
        } catch (Exception e) {
            log.error("Failed to check online status for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Get all online users
     */
    public Set<Object> getOnlineUsers() {
        try {
            return redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        } catch (Exception e) {
            log.error("Failed to get online users", e);
            return Set.of();
        }
    }
}
