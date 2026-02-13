package com.whatsapp.chat.application.service;

import com.whatsapp.chat.application.dto.ConversationDto;
import com.whatsapp.chat.application.dto.CreateConversationRequest;
import com.whatsapp.chat.application.mapper.ConversationMapper;
import com.whatsapp.chat.domain.model.Conversation;
import com.whatsapp.chat.domain.model.ConversationType;
import com.whatsapp.chat.domain.model.vo.ConversationId;
import com.whatsapp.chat.domain.model.vo.Participant;
import com.whatsapp.chat.domain.repository.ConversationRepository;
import com.whatsapp.chat.infrastructure.cache.InboxCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Conversation Service
 *
 * Handles conversation management including creation, retrieval, and updates.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final InboxCacheService cacheService;
    private final ConversationMapper conversationMapper;

    /**
     * Create a new conversation
     */
    @Transactional
    public ConversationDto createConversation(CreateConversationRequest request) {
        log.info("Creating new conversation of type: {}", request.getType());

        Conversation conversation;

        if ("ONE_TO_ONE".equals(request.getType())) {
            // Check if conversation already exists between these users
            var existing = conversationRepository.findOneToOneConversation(
                    request.getParticipant1Id(),
                    request.getParticipant2Id());

            if (existing.isPresent()) {
                log.info("Conversation already exists: {}", existing.get().getId());
                return conversationMapper.toDto(existing.get());
            }

            conversation = Conversation.createOneToOne(
                    request.getParticipant1Id(),
                    request.getParticipant1Name(),
                    request.getParticipant2Id(),
                    request.getParticipant2Name());
        } else {
            // Group conversation
            List<Participant> additionalParticipants = request.getAdditionalParticipants().stream()
                    .map(p -> Participant.create(p.getUserId(), p.getDisplayName()))
                    .collect(Collectors.toList());

            conversation = Conversation.createGroup(
                    request.getName(),
                    request.getDescription(),
                    request.getCreatorId(),
                    request.getCreatorName(),
                    additionalParticipants);
        }

        Conversation savedConversation = conversationRepository.save(conversation);

        log.info("Conversation created successfully: {}", savedConversation.getId());

        return conversationMapper.toDto(savedConversation);
    }

    /**
     * Get conversation by ID
     */
    @Transactional(readOnly = true)
    public ConversationDto getConversation(String conversationId, String userId) {
        log.debug("Getting conversation: {}", conversationId);

        Conversation conversation = conversationRepository.findById(ConversationId.of(conversationId))
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        // Validate user is participant
        if (!conversation.isParticipant(userId)) {
            throw new IllegalArgumentException("User is not a participant in this conversation");
        }

        return conversationMapper.toDto(conversation);
    }

    /**
     * Get all conversations for a user
     */
    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations(String userId) {
        log.debug("Getting conversations for user: {}", userId);

        List<Conversation> conversations = conversationRepository.findByParticipantId(userId);

        return conversations.stream()
                .map(conversationMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Mark conversation as read for a user
     */
    @Transactional
    public void markAsRead(String conversationId, String userId) {
        log.info("Marking conversation as read: {} for user: {}", conversationId, userId);

        Conversation conversation = conversationRepository.findById(ConversationId.of(conversationId))
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        // Validate user is participant
        if (!conversation.isParticipant(userId)) {
            throw new IllegalArgumentException("User is not a participant in this conversation");
        }

        conversation.resetUnreadCount(userId);
        conversationRepository.save(conversation);

        // Invalidate cache
        cacheService.evictConversation(conversationId);

        log.info("Conversation marked as read: {}", conversationId);
    }

    /**
     * Add participant to group conversation
     */
    @Transactional
    public ConversationDto addParticipant(String conversationId, String userId, String newParticipantId,
            String newParticipantName) {
        log.info("Adding participant {} to conversation: {}", newParticipantId, conversationId);

        Conversation conversation = conversationRepository.findById(ConversationId.of(conversationId))
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        // Validate requester is admin
        if (!conversation.isAdmin(userId)) {
            throw new IllegalArgumentException("Only admins can add participants");
        }

        // Add participant
        Participant newParticipant = Participant.create(newParticipantId, newParticipantName);
        conversation.addParticipant(newParticipant);

        Conversation updatedConversation = conversationRepository.save(conversation);

        // Invalidate cache
        cacheService.evictConversation(conversationId);

        log.info("Participant added successfully to conversation: {}", conversationId);

        return conversationMapper.toDto(updatedConversation);
    }

    /**
     * Remove participant from group conversation
     */
    @Transactional
    public void removeParticipant(String conversationId, String userId, String participantToRemove) {
        log.info("Removing participant {} from conversation: {}", participantToRemove, conversationId);

        Conversation conversation = conversationRepository.findById(ConversationId.of(conversationId))
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        // Validate requester is admin or removing themselves
        if (!conversation.isAdmin(userId) && !userId.equals(participantToRemove)) {
            throw new IllegalArgumentException("Only admins can remove other participants");
        }

        conversation.removeParticipant(participantToRemove);
        conversationRepository.save(conversation);

        // Invalidate cache
        cacheService.evictConversation(conversationId);

        log.info("Participant removed successfully from conversation: {}", conversationId);
    }
}
