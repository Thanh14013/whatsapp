package com.whatsapp.chat.infrastructure.persistence.postgres.repository;

import com.whatsapp.chat.domain.model.Conversation;
import com.whatsapp.chat.domain.model.ConversationType;
import com.whatsapp.chat.domain.model.vo.ConversationId;
import com.whatsapp.chat.domain.model.vo.Participant;
import com.whatsapp.chat.domain.repository.ConversationRepository;
import com.whatsapp.chat.infrastructure.persistence.postgres.entity.ConversationEntity;
import com.whatsapp.chat.infrastructure.persistence.postgres.entity.ConversationParticipantEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Conversation Repository Implementation
 *
 * Adapts the domain {@link ConversationRepository} interface to the JPA
 * infrastructure using {@link ConversationJpaRepository}.
 *
 * Mapping strategy:
 *  - ConversationEntity       ↔ Conversation domain model
 *  - ConversationParticipantEntity ↔ Participant value object
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Primary
@Repository
@RequiredArgsConstructor
public class ConversationRepositoryImpl implements ConversationRepository {

    private final ConversationJpaRepository jpaRepository;

    // ---------------------------------------------------------------
    // ConversationRepository implementation
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public Conversation save(Conversation conversation) {
        ConversationEntity entity = toEntity(conversation);
        ConversationEntity saved  = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findById(ConversationId conversationId) {
        return jpaRepository.findById(conversationId.getValue()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findOneToOneConversation(String user1Id, String user2Id) {
        return jpaRepository.findOneToOneByParticipants(user1Id, user2Id).map(this::toDomain);
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public Optional<Conversation> findByParticipants(String user1Id, String user2Id) {
        return findOneToOneConversation(user1Id, user2Id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> findByParticipantId(String userId) {
        return jpaRepository.findByParticipantUserId(userId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> findByUserId(String userId, int offset, int limit) {
        return jpaRepository.findByParticipantUserIdPaged(userId, offset, limit)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByParticipants(String user1Id, String user2Id) {
        return jpaRepository.existsOneToOneByParticipants(user1Id, user2Id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserId(String userId) {
        return jpaRepository.countByParticipantUserId(userId);
    }

    @Override
    @Transactional
    public void delete(ConversationId conversationId) {
        jpaRepository.deleteById(conversationId.getValue());
    }

    // ---------------------------------------------------------------
    // Mapping helpers
    // ---------------------------------------------------------------

    private ConversationEntity toEntity(Conversation domain) {
        // Build participant entities
        List<ConversationParticipantEntity> participantEntities = domain.getParticipants().stream()
                .map(p -> ConversationParticipantEntity.builder()
                        .userId(p.getUserId())
                        .displayName(p.getDisplayName())
                        .admin(p.isAdmin())
                        .unreadCount(domain.getUnreadCount(p.getUserId()))
                        .build())
                .collect(Collectors.toList());

        ConversationEntity entity = ConversationEntity.builder()
                .id(domain.getId().getValue())
                .type(domain.getType().name())
                .name(domain.getName())
                .description(domain.getDescription())
                .avatarUrl(domain.getAvatarUrl())
                .lastMessageId(domain.getLastMessageId())
                .lastMessageTimestamp(domain.getLastMessageTimestamp())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .participants(participantEntities)
                .build();

        // Wire back-reference
        participantEntities.forEach(pe -> pe.setConversation(entity));
        return entity;
    }

    private Conversation toDomain(ConversationEntity entity) {
        List<Participant> participants = entity.getParticipants().stream()
                .filter(p -> p.getLeftAt() == null)
                .map(p -> Participant.of(p.getUserId(), p.getDisplayName(), p.isAdmin()))
                .collect(Collectors.toList());

        Map<String, Integer> unreadCounts = new HashMap<>();
        entity.getParticipants().forEach(p -> unreadCounts.put(p.getUserId(), p.getUnreadCount()));

        return Conversation.reconstitute(
                ConversationId.of(entity.getId()),
                ConversationType.valueOf(entity.getType()),
                entity.getName(),
                entity.getDescription(),
                entity.getAvatarUrl(),
                participants,
                entity.getLastMessageId(),
                entity.getLastMessageTimestamp(),
                unreadCounts,
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

