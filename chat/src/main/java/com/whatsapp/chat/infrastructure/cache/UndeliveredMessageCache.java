package com.whatsapp.chat.infrastructure.cache;

import com.whatsapp.chat.application.dto.MessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Undelivered Message Cache
 *
 * Manages a per-user inbox in Redis that temporarily stores messages
 * waiting to be delivered while the recipient is offline.
 *
 * Data structure:
 *   Key   : "inbox:{receiverId}"           (Redis List)
 *   Value : JSON-serialised {@link MessageDto}
 *   TTL   : 7 days (configurable)
 *
 * Workflow:
 *  1. When a message is sent and the recipient is OFFLINE →
 *       push {@link MessageDto} to inbox list via {@link #pushMessage}.
 *  2. When the recipient comes ONLINE →
 *       pop all messages via {@link #popAllMessages} and deliver them.
 *  3. The list is also used for unread-count badges via {@link #getMessageCount}.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UndeliveredMessageCache {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String INBOX_KEY_PREFIX = "inbox:";
    /** 7-day TTL – after this period undelivered messages are evicted. */
    private static final Duration INBOX_TTL = Duration.ofDays(7);

    // ---------------------------------------------------------------
    // Write operations
    // ---------------------------------------------------------------

    /**
     * Push a message into the recipient's inbox.
     *
     * @param receiverId receiver's user ID
     * @param message    the message DTO to cache
     */
    public void pushMessage(String receiverId, MessageDto message) {
        try {
            String key = inboxKey(receiverId);
            redisTemplate.opsForList().rightPush(key, message);
            redisTemplate.expire(key, INBOX_TTL);
            log.debug("Pushed message {} to inbox of user {}", message.getId(), receiverId);
        } catch (Exception e) {
            log.error("Failed to push message to inbox for user {}: {}", receiverId, e.getMessage(), e);
        }
    }

    /**
     * Pop (drain) all messages from the recipient's inbox.
     * Atomically removes every entry and returns them in FIFO order.
     *
     * @param receiverId receiver's user ID
     * @return list of pending messages, oldest first
     */
    @SuppressWarnings("unchecked")
    public List<MessageDto> popAllMessages(String receiverId) {
        try {
            String key = inboxKey(receiverId);
            Long size = redisTemplate.opsForList().size(key);
            if (size == null || size == 0) {
                return Collections.emptyList();
            }

            List<Object> raw = redisTemplate.opsForList().range(key, 0, size - 1);
            redisTemplate.delete(key);

            if (raw == null) return Collections.emptyList();

            return raw.stream()
                    .filter(o -> o instanceof MessageDto)
                    .map(o -> (MessageDto) o)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to pop messages from inbox for user {}: {}", receiverId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Remove a single message from the inbox (e.g. after successful delivery).
     *
     * @param receiverId receiver's user ID
     * @param messageId  ID of the message to remove
     */
    public void removeMessage(String receiverId, String messageId) {
        try {
            String key = inboxKey(receiverId);
            Long size = redisTemplate.opsForList().size(key);
            if (size == null || size == 0) return;

            List<Object> items = redisTemplate.opsForList().range(key, 0, size - 1);
            if (items == null) return;

            items.stream()
                    .filter(o -> o instanceof MessageDto)
                    .map(o -> (MessageDto) o)
                    .filter(m -> messageId.equals(m.getId()))
                    .findFirst()
                    .ifPresent(m -> redisTemplate.opsForList().remove(key, 1, m));

            log.debug("Removed message {} from inbox of user {}", messageId, receiverId);
        } catch (Exception e) {
            log.error("Failed to remove message {} from inbox for user {}: {}", messageId, receiverId, e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Read operations
    // ---------------------------------------------------------------

    /**
     * Get the number of pending (undelivered) messages for a user.
     *
     * @param receiverId receiver's user ID
     * @return number of pending messages
     */
    public long getMessageCount(String receiverId) {
        try {
            Long size = redisTemplate.opsForList().size(inboxKey(receiverId));
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("Failed to get message count for user {}: {}", receiverId, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * Check whether the inbox is non-empty.
     *
     * @param receiverId receiver's user ID
     * @return {@code true} if there are pending messages
     */
    public boolean hasPendingMessages(String receiverId) {
        return getMessageCount(receiverId) > 0;
    }

    /**
     * Retrieve all pending messages without removing them (peek).
     *
     * @param receiverId receiver's user ID
     * @return list of pending messages, oldest first
     */
    @SuppressWarnings("unchecked")
    public List<MessageDto> peekMessages(String receiverId) {
        try {
            String key = inboxKey(receiverId);
            Long size = redisTemplate.opsForList().size(key);
            if (size == null || size == 0) return Collections.emptyList();

            List<Object> raw = redisTemplate.opsForList().range(key, 0, size - 1);
            if (raw == null) return Collections.emptyList();

            return raw.stream()
                    .filter(o -> o instanceof MessageDto)
                    .map(o -> (MessageDto) o)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to peek messages for user {}: {}", receiverId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ---------------------------------------------------------------
    // Maintenance
    // ---------------------------------------------------------------

    /**
     * Clear the entire inbox for a user (e.g. account deletion).
     *
     * @param receiverId receiver's user ID
     */
    public void clearInbox(String receiverId) {
        try {
            redisTemplate.delete(inboxKey(receiverId));
            log.debug("Cleared inbox for user {}", receiverId);
        } catch (Exception e) {
            log.error("Failed to clear inbox for user {}: {}", receiverId, e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String inboxKey(String receiverId) {
        return INBOX_KEY_PREFIX + receiverId;
    }
}

