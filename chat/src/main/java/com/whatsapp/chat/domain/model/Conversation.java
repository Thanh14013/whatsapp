package com.whatsapp.chat.domain.model;

import com.whatsapp.chat.domain.model.vo.ConversationId;
import com.whatsapp.chat.domain.model.vo.Participant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Conversation Domain Model (Aggregate Root)
 *
 * Represents a ONE_TO_ONE or GROUP conversation.
 * Contains all business logic for conversation lifecycle.
 *
 * Business Rules:
 * - ONE_TO_ONE: exactly 2 participants; uniqueness enforced by sorted pair key
 * - GROUP: 1-N participants; creator is automatically admin
 * - Only admins may add/remove other participants from groups
 * - lastMessageId / lastMessageTimestamp are updated on every new message
 * - Per-user unread counts are tracked in a map
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Conversation {

    private ConversationId id;
    private ConversationType type;
    private String name;
    private String description;
    private String avatarUrl;
    private List<Participant> participants;
    private String lastMessageId;
    private Instant lastMessageTimestamp;
    private Map<String, Integer> unreadCounts; // userId → unread count
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Reconstitute a Conversation from persistent storage.
     * For use by repository implementations only.
     */
    public static Conversation reconstitute(
            ConversationId id,
            ConversationType type,
            String name,
            String description,
            String avatarUrl,
            List<Participant> participants,
            String lastMessageId,
            Instant lastMessageTimestamp,
            Map<String, Integer> unreadCounts,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        return new Conversation(id, type, name, description, avatarUrl, participants,
                lastMessageId, lastMessageTimestamp, unreadCounts, active, createdAt, updatedAt);
    }

    // ---------------------------------------------------------------
    // Factory methods
    // ---------------------------------------------------------------

    /**
     * Create a ONE_TO_ONE conversation between two users.
     */
    public static Conversation createOneToOne(
            String user1Id, String user1Name,
            String user2Id, String user2Name) {

        validateUserId(user1Id);
        validateUserId(user2Id);
        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("Cannot create a conversation with yourself");
        }

        List<Participant> participants = new ArrayList<>();
        participants.add(Participant.create(user1Id, user1Name));
        participants.add(Participant.create(user2Id, user2Name));

        Instant now = Instant.now();
        Map<String, Integer> unread = new HashMap<>();
        unread.put(user1Id, 0);
        unread.put(user2Id, 0);

        // Derive a stable display name from the pair (each user sees the other's name)
        String pairName = user1Id.compareTo(user2Id) < 0
                ? user1Name + ":" + user2Name
                : user2Name + ":" + user1Name;

        return new Conversation(
                ConversationId.generate(),
                ConversationType.ONE_TO_ONE,
                pairName,
                null,
                null,
                participants,
                null,
                null,
                unread,
                true,
                now,
                now
        );
    }

    /**
     * Create a GROUP conversation.
     */
    public static Conversation createGroup(
            String name,
            String description,
            String creatorId,
            String creatorName,
            List<Participant> additionalParticipants) {

        Objects.requireNonNull(name, "Group name cannot be null");
        validateUserId(creatorId);

        List<Participant> allParticipants = new ArrayList<>();
        allParticipants.add(Participant.createAdmin(creatorId, creatorName));
        if (additionalParticipants != null) {
            allParticipants.addAll(additionalParticipants);
        }

        Map<String, Integer> unread = new HashMap<>();
        allParticipants.forEach(p -> unread.put(p.getUserId(), 0));

        Instant now = Instant.now();
        return new Conversation(
                ConversationId.generate(),
                ConversationType.GROUP,
                name,
                description,
                null,
                allParticipants,
                null,
                null,
                unread,
                true,
                now,
                now
        );
    }

    // ---------------------------------------------------------------
    // Business methods
    // ---------------------------------------------------------------

    /**
     * Update metadata after a new message was sent.
     *
     * @param messageId  ID of the new message (Snowflake string)
     * @param sentAt     timestamp the message was sent
     */
    public void updateLastMessage(String messageId, Instant sentAt) {
        Objects.requireNonNull(messageId, "Message ID cannot be null");
        this.lastMessageId = messageId;
        this.lastMessageTimestamp = sentAt;
        this.updatedAt = Instant.now();
    }

    /**
     * Increment the unread count for a specific user.
     *
     * @param userId the user to increment for
     */
    public void incrementUnreadCount(String userId) {
        unreadCounts.merge(userId, 1, Integer::sum);
        this.updatedAt = Instant.now();
    }

    /**
     * Reset (clear) the unread count for a user (they read all messages).
     *
     * @param userId the user who read messages
     */
    public void resetUnreadCount(String userId) {
        unreadCounts.put(userId, 0);
        this.updatedAt = Instant.now();
    }

    /**
     * Get the unread message count for a user.
     *
     * @param userId the user to query
     * @return unread count (0 if not tracked)
     */
    public int getUnreadCount(String userId) {
        return unreadCounts.getOrDefault(userId, 0);
    }

    /**
     * Check if a user is a participant.
     */
    public boolean isParticipant(String userId) {
        return participants.stream().anyMatch(p -> p.getUserId().equals(userId));
    }

    /**
     * Check if a user is an admin.
     */
    public boolean isAdmin(String userId) {
        return participants.stream()
                .anyMatch(p -> p.getUserId().equals(userId) && p.isAdmin());
    }

    /**
     * Add a new participant (group only).
     */
    public void addParticipant(Participant participant) {
        if (type != ConversationType.GROUP) {
            throw new IllegalStateException("Cannot add participants to a ONE_TO_ONE conversation");
        }
        if (isParticipant(participant.getUserId())) {
            throw new IllegalArgumentException("User is already a participant: " + participant.getUserId());
        }
        participants.add(participant);
        unreadCounts.put(participant.getUserId(), 0);
        this.updatedAt = Instant.now();
    }

    /**
     * Remove a participant (group only).
     */
    public void removeParticipant(String userId) {
        if (type != ConversationType.GROUP) {
            throw new IllegalStateException("Cannot remove participants from a ONE_TO_ONE conversation");
        }
        participants.removeIf(p -> p.getUserId().equals(userId));
        unreadCounts.remove(userId);
        this.updatedAt = Instant.now();
    }

    /**
     * Get the other participant ID for a ONE_TO_ONE conversation.
     */
    public String getOtherParticipantId(String userId) {
        if (type != ConversationType.ONE_TO_ONE) {
            throw new IllegalStateException("Not a ONE_TO_ONE conversation");
        }
        return participants.stream()
                .map(Participant::getUserId)
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant: " + userId));
    }

    /**
     * Generate a stable conversation key for ONE_TO_ONE uniqueness check.
     */
    public static String generateConversationKey(String user1Id, String user2Id) {
        List<String> ids = Arrays.asList(user1Id, user2Id);
        ids.sort(String::compareTo);
        return ids.get(0) + ":" + ids.get(1);
    }

    /**
     * Find a conversation between two participants (ONE_TO_ONE).
     * Delegates to {@link #generateConversationKey} for stable comparison.
     */
    public boolean matchesParticipants(String user1Id, String user2Id) {
        if (type != ConversationType.ONE_TO_ONE) return false;
        List<String> mine = participants.stream()
                .map(Participant::getUserId)
                .sorted()
                .collect(Collectors.toList());
        List<String> query = Arrays.asList(user1Id, user2Id);
        query.sort(String::compareTo);
        return mine.equals(query);
    }

    // ---------------------------------------------------------------
    // Validation helpers
    // ---------------------------------------------------------------

    private static void validateUserId(String userId) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        if (userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
    }

    // ---------------------------------------------------------------
    // equals / hashCode
    // ---------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}