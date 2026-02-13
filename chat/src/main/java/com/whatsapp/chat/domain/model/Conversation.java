package com.whatsapp.chat.domain.model;

import com.whatsapp.chat.domain.model.vo.ConversationId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Conversation Domain Model (Aggregate Root)
 *
 * Represents a conversation between two users.
 * Contains conversation metadata and business logic.
 *
 * Business Rules:
 * - Conversation must have exactly 2 participants
 * - Each pair of users can have only one conversation
 * - Conversation tracks last message and timestamp
 * - Participants are sorted alphabetically for uniqueness
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Conversation {

    private ConversationId id;
    private String participant1Id; // Alphabetically first
    private String participant2Id; // Alphabetically second
    private String lastMessageId;
    private String lastMessageContent;
    private Instant lastMessageAt;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Factory method to create a new Conversation
     */
    public static Conversation create(String user1Id, String user2Id) {
        validateParticipants(user1Id, user2Id);

        // Sort participants alphabetically for uniqueness
        List<String> participants = Arrays.asList(user1Id, user2Id);
        participants.sort(String::compareTo);

        Instant now = Instant.now();

        return new Conversation(
                ConversationId.generate(),
                participants.get(0),
                participants.get(1),
                null,
                null,
                null,
                now,
                now
        );
    }

    /**
     * Update last message
     */
    public void updateLastMessage(String messageId, String messageContent) {
        Objects.requireNonNull(messageId, "Message ID cannot be null");

        this.lastMessageId = messageId;
        this.lastMessageContent = messageContent;
        this.lastMessageAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Check if user is participant
     */
    public boolean isParticipant(String userId) {
        return participant1Id.equals(userId) || participant2Id.equals(userId);
    }

    /**
     * Get other participant ID
     */
    public String getOtherParticipantId(String userId) {
        if (participant1Id.equals(userId)) {
            return participant2Id;
        } else if (participant2Id.equals(userId)) {
            return participant1Id;
        } else {
            throw new IllegalArgumentException("User is not a participant: " + userId);
        }
    }

    /**
     * Generate conversation key for uniqueness
     */
    public static String generateConversationKey(String user1Id, String user2Id) {
        List<String> participants = Arrays.asList(user1Id, user2Id);
        participants.sort(String::compareTo);
        return participants.get(0) + ":" + participants.get(1);
    }

    /**
     * Validate participants
     */
    private static void validateParticipants(String user1Id, String user2Id) {
        Objects.requireNonNull(user1Id, "User 1 ID cannot be null");
        Objects.requireNonNull(user2Id, "User 2 ID cannot be null");

        if (user1Id.isBlank()) {
            throw new IllegalArgumentException("User 1 ID cannot be empty");
        }

        if (user2Id.isBlank()) {
            throw new IllegalArgumentException("User 2 ID cannot be empty");
        }

        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("Participants must be different users");
        }
    }

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