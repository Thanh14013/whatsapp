package com.whatsapp.chat.domain.repository;

import com.whatsapp.chat.domain.model.Conversation;
import com.whatsapp.chat.domain.model.vo.ConversationId;

import java.util.List;
import java.util.Optional;

/**
 * Conversation Repository Interface (Domain Layer)
 *
 * Defines repository operations for Conversation aggregate.
 * Implementation is in infrastructure layer (PostgreSQL).
 *
 * @author WhatsApp Clone Team
 */
public interface ConversationRepository {

    /**
     * Save or update conversation
     */
    Conversation save(Conversation conversation);

    /**
     * Find conversation by ID
     */
    Optional<Conversation> findById(ConversationId conversationId);

    /**
     * Find conversation by participants
     */
    Optional<Conversation> findByParticipants(String user1Id, String user2Id);

    /**
     * Find all conversations for a user
     */
    List<Conversation> findByUserId(String userId);

    /**
     * Find all conversations for a user with pagination
     */
    List<Conversation> findByUserId(String userId, int offset, int limit);

    /**
     * Check if conversation exists between two users
     */
    boolean existsByParticipants(String user1Id, String user2Id);

    /**
     * Count conversations for a user
     */
    long countByUserId(String userId);

    /**
     * Delete conversation
     */
    void delete(ConversationId conversationId);
}