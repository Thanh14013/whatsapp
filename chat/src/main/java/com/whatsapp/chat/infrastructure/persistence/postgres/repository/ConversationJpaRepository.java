package com.whatsapp.chat.infrastructure.persistence.postgres.repository;

import com.whatsapp.chat.infrastructure.persistence.postgres.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Conversation JPA Repository
 *
 * Spring Data JPA repository for {@link ConversationEntity}.
 * Provides CRUD operations and domain-specific queries against PostgreSQL.
 *
 * Schema:
 *  - conversations                (id, type, name, …)
 *  - conversation_participants    (conversation_id, user_id, is_admin, unread_count, …)
 *
 * @author WhatsApp Clone Team
 */
@Repository
public interface ConversationJpaRepository extends JpaRepository<ConversationEntity, String> {

    /**
     * Find all conversations a user participates in, ordered by last activity.
     */
    @Query("""
            SELECT DISTINCT c FROM ConversationEntity c
              JOIN c.participants p
             WHERE p.userId = :userId AND p.leftAt IS NULL
             ORDER BY c.lastMessageTimestamp DESC NULLS LAST
            """)
    List<ConversationEntity> findByParticipantUserId(@Param("userId") String userId);

    /**
     * Paginated variant.
     */
    @Query(value = """
            SELECT DISTINCT c.* FROM conversations c
              JOIN conversation_participants p ON p.conversation_id = c.id
             WHERE p.user_id = :userId AND p.left_at IS NULL
             ORDER BY c.last_message_timestamp DESC NULLS LAST
             LIMIT :lim OFFSET :off
            """, nativeQuery = true)
    List<ConversationEntity> findByParticipantUserIdPaged(
            @Param("userId") String userId,
            @Param("off")    int offset,
            @Param("lim")    int limit);

    /**
     * Find a ONE_TO_ONE conversation between exactly two users.
     * Both users must be current (leftAt IS NULL) participants.
     */
    @Query("""
            SELECT c FROM ConversationEntity c
             WHERE c.type = 'ONE_TO_ONE'
               AND EXISTS (
                   SELECT 1 FROM ConversationParticipantEntity p1
                    WHERE p1.conversation = c AND p1.userId = :u1 AND p1.leftAt IS NULL
               )
               AND EXISTS (
                   SELECT 1 FROM ConversationParticipantEntity p2
                    WHERE p2.conversation = c AND p2.userId = :u2 AND p2.leftAt IS NULL
               )
            """)
    Optional<ConversationEntity> findOneToOneByParticipants(
            @Param("u1") String user1Id,
            @Param("u2") String user2Id);

    /**
     * Check if a ONE_TO_ONE conversation already exists between two users.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM ConversationEntity c
             WHERE c.type = 'ONE_TO_ONE'
               AND EXISTS (
                   SELECT 1 FROM ConversationParticipantEntity p1
                    WHERE p1.conversation = c AND p1.userId = :u1 AND p1.leftAt IS NULL
               )
               AND EXISTS (
                   SELECT 1 FROM ConversationParticipantEntity p2
                    WHERE p2.conversation = c AND p2.userId = :u2 AND p2.leftAt IS NULL
               )
            """)
    boolean existsOneToOneByParticipants(
            @Param("u1") String user1Id,
            @Param("u2") String user2Id);

    /**
     * Count conversations for a user.
     */
    @Query("""
            SELECT COUNT(DISTINCT c) FROM ConversationEntity c
              JOIN c.participants p
             WHERE p.userId = :userId AND p.leftAt IS NULL
            """)
    long countByParticipantUserId(@Param("userId") String userId);
}
