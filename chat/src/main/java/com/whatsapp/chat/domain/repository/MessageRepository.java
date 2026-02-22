package com.whatsapp.chat.domain.repository;

import com.whatsapp.chat.domain.model.Message;
import com.whatsapp.chat.domain.model.vo.ConversationId;
import com.whatsapp.chat.domain.model.vo.MessageId;

import java.util.List;
import java.util.Optional;

/**
 * Message Repository Interface (Domain Layer)
 *
 * Defines repository operations for Message aggregate.
 * Implementation is in infrastructure layer (MongoDB).
 *
 * @author WhatsApp Clone Team
 */
public interface MessageRepository {

    /**
     * Save or update message
     */
    Message save(Message message);

    /**
     * Find message by ID
     */
    Optional<Message> findById(MessageId messageId);

    /**
     * Find messages by conversation ID
     */
    List<Message> findByConversationId(ConversationId conversationId, int limit);

    /**
     * Find messages by conversation ID with pagination
     */
    List<Message> findByConversationId(ConversationId conversationId, int offset, int limit);

    /**
     * Find undelivered messages for receiver
     */
    List<Message> findUndeliveredMessages(String receiverId);

    /**
     * Find messages by sender and receiver
     */
    List<Message> findBySenderAndReceiver(String senderId, String receiverId, int limit);

    /**
     * Count messages in conversation
     */
    long countByConversationId(ConversationId conversationId);

    /**
     * Count undelivered messages for user
     */
    long countUndeliveredMessages(String receiverId);

    /**
     * Delete message (soft delete)
     */
    void delete(MessageId messageId);

    /**
     * Delete all messages in conversation
     */
    void deleteByConversationId(ConversationId conversationId);
}