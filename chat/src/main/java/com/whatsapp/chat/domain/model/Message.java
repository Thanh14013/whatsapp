package com.whatsapp.chat.domain.model;

import com.whatsapp.chat.domain.model.vo.ConversationId;
import com.whatsapp.chat.domain.model.vo.MessageContent;
import com.whatsapp.chat.domain.model.vo.MessageId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/**
 * Message Domain Model (Aggregate Root)
 *
 * Represents a message in a conversation.
 * Contains business logic for message lifecycle.
 *
 * Business Rules:
 * - Message must have sender and receiver
 * - Message content cannot be empty
 * - Message can be delivered, read, or deleted
 * - Sender cannot delete message after 1 hour
 * - Message status transitions: SENT → DELIVERED → READ
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Message {

    private MessageId id;
    private ConversationId conversationId;
    private String senderId;
    private String receiverId;
    private MessageContent content;
    private MessageStatus status;
    private boolean deleted;
    private Instant createdAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant deletedAt;

    /**
     * Factory method to create a new Message
     */
    public static Message create(
            ConversationId conversationId,
            String senderId,
            String receiverId,
            MessageContent content) {

        validateParticipants(senderId, receiverId);
        Objects.requireNonNull(conversationId, "Conversation ID cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");

        Instant now = Instant.now();

        return new Message(
                MessageId.generate(),
                conversationId,
                senderId,
                receiverId,
                content,
                MessageStatus.SENT,
                false,
                now,
                null,
                null,
                null
        );
    }

    /**
     * Mark message as delivered
     */
    public void markAsDelivered() {
        if (this.status == MessageStatus.SENT) {
            this.status = MessageStatus.DELIVERED;
            this.deliveredAt = Instant.now();
        }
    }

    /**
     * Mark message as read
     */
    public void markAsRead() {
        if (this.status == MessageStatus.DELIVERED || this.status == MessageStatus.SENT) {
            this.status = MessageStatus.READ;
            this.readAt = Instant.now();

            // Auto-mark as delivered if not already
            if (this.deliveredAt == null) {
                this.deliveredAt = this.readAt;
            }
        }
    }

    /**
     * Delete message (soft delete)
     *
     * Business Rule: Can only delete within 1 hour of sending
     */
    public void delete(String userId) {
        // Check if user is the sender
        if (!this.senderId.equals(userId)) {
            throw new IllegalStateException("Only sender can delete the message");
        }

        // Check if message can still be deleted (within 1 hour)
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        if (this.createdAt.isBefore(oneHourAgo)) {
            throw new IllegalStateException("Cannot delete message after 1 hour");
        }

        if (this.deleted) {
            throw new IllegalStateException("Message is already deleted");
        }

        this.deleted = true;
        this.deletedAt = Instant.now();
    }

    /**
     * Check if message is delivered
     */
    public boolean isDelivered() {
        return this.status == MessageStatus.DELIVERED || this.status == MessageStatus.READ;
    }

    /**
     * Check if message is read
     */
    public boolean isRead() {
        return this.status == MessageStatus.READ;
    }

    /**
     * Check if message can be deleted
     */
    public boolean canDelete() {
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        return !this.deleted && this.createdAt.isAfter(oneHourAgo);
    }

    /**
     * Validate participants
     */
    private static void validateParticipants(String senderId, String receiverId) {
        Objects.requireNonNull(senderId, "Sender ID cannot be null");
        Objects.requireNonNull(receiverId, "Receiver ID cannot be null");

        if (senderId.isBlank()) {
            throw new IllegalArgumentException("Sender ID cannot be empty");
        }

        if (receiverId.isBlank()) {
            throw new IllegalArgumentException("Receiver ID cannot be empty");
        }

        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Sender and receiver cannot be the same");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}