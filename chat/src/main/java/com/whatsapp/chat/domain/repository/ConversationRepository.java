package com.whatsapp.chat.domain.repository;

import com.whatsapp.chat.domain.model.Conversation;
import com.whatsapp.chat.domain.model.vo.ConversationId;

import java.util.List;
import java.util.Optional;

/**
 * Conversation Repository Interface (Domain Layer)
 *
 * Defines repository operations for the Conversation aggregate.
 * Implementation lives in the infrastructure layer (PostgreSQL via JPA).
 *
 * @author WhatsApp Clone Team
 */
public interface ConversationRepository {

    /** Save or update a conversation. */
    Conversation save(Conversation conversation);

    /** Find a conversation by its unique ID. */
    Optional<Conversation> findById(ConversationId conversationId);

    /**
     * Find a ONE_TO_ONE conversation between two specific users.
     * Used to enforce "one conversation per pair" invariant.
     */
    Optional<Conversation> findOneToOneConversation(String user1Id, String user2Id);

    /** @deprecated Use {@link #findOneToOneConversation} for ONE_TO_ONE lookups. */
    @Deprecated
    Optional<Conversation> findByParticipants(String user1Id, String user2Id);

    /** Find all conversations a user participates in, ordered by last activity. */
    List<Conversation> findByParticipantId(String userId);

    /** Paginated variant of {@link #findByParticipantId}. */
    List<Conversation> findByUserId(String userId, int offset, int limit);

    /** Check whether a ONE_TO_ONE conversation already exists between two users. */
    boolean existsByParticipants(String user1Id, String user2Id);

    /** Count conversations for a user. */
    long countByUserId(String userId);

    /** Delete a conversation by ID. */
    void delete(ConversationId conversationId);
}