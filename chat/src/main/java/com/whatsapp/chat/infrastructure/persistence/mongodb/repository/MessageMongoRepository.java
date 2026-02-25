package com.whatsapp.chat.infrastructure.persistence.mongodb.repository;

import com.whatsapp.chat.infrastructure.persistence.mongodb.document.MessageDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Message MongoDB Repository
 *
 * Spring Data MongoDB repository for {@link MessageDocument}.
 * Provides CRUD operations and domain-specific query methods.
 *
 * Indexes (defined on the document class):
 *  - conversation_created_idx  (conversationId ASC, createdAt DESC)  – history queries
 *  - receiver_status_idx       (receiverId ASC, status ASC)          – inbox queries
 *
 * @author WhatsApp Clone Team
 */
@Repository
public interface MessageMongoRepository extends MongoRepository<MessageDocument, String> {

    /**
     * Find all messages in a conversation, newest first.
     *
     * @param conversationId the conversation ID
     * @param pageable       pagination parameters
     * @return page of message documents
     */
    List<MessageDocument> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    /**
     * Find all messages in a conversation with no pagination limit
     * (internal use only – prefer the paginated variant).
     *
     * @param conversationId the conversation ID
     * @return all messages in the conversation
     */
    List<MessageDocument> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    /**
     * Find undelivered (SENT) messages for a recipient.
     * Used for inbox sync when the user comes online.
     *
     * @param receiverId the recipient's user ID
     * @param status     should be "SENT"
     * @return list of undelivered message documents
     */
    List<MessageDocument> findByReceiverIdAndStatusOrderByCreatedAtAsc(String receiverId, String status);

    /**
     * Find messages exchanged between two users (any direction).
     *
     * @param senderId   one participant
     * @param receiverId other participant
     * @param pageable   pagination parameters
     * @return list of message documents
     */
    @Query("{ $or: [ { senderId: ?0, receiverId: ?1 }, { senderId: ?1, receiverId: ?0 } ] }")
    List<MessageDocument> findBetweenUsers(String senderId, String receiverId, Pageable pageable);

    /**
     * Count messages in a conversation.
     *
     * @param conversationId the conversation ID
     * @return total message count
     */
    long countByConversationId(String conversationId);

    /**
     * Count undelivered messages for a recipient.
     *
     * @param receiverId the recipient's user ID
     * @param status     should be "SENT"
     * @return count of undelivered messages
     */
    long countByReceiverIdAndStatus(String receiverId, String status);

    /**
     * Delete all messages belonging to a conversation.
     *
     * @param conversationId the conversation ID
     */
    void deleteByConversationId(String conversationId);
}

