package com.whatsapp.chat.application.service;

import com.whatsapp.chat.application.dto.MessageDto;
import com.whatsapp.chat.application.mapper.MessageMapper;
import com.whatsapp.chat.domain.model.Message;
import com.whatsapp.chat.domain.model.vo.ConversationId;
import com.whatsapp.chat.domain.model.vo.MessageId;
import com.whatsapp.chat.domain.repository.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Message Query Service
 *
 * Dedicated read-only service for all message query operations.
 * Follows CQRS principle – all writes are handled by {@link ChatApplicationService}.
 *
 * Responsibilities:
 * - Fetch single messages by ID
 * - Fetch paginated conversation history
 * - Fetch undelivered messages for a recipient
 * - Count messages and undelivered items
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class MessageQueryService {

    private final MessageRepository messageRepository;
    private final MessageMapper      messageMapper;

    /**
     * Find a message by its unique ID.
     *
     * @param messageId the message ID
     * @return an {@link Optional} containing the message DTO, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<MessageDto> findById(String messageId) {
        log.debug("Querying message by id: {}", messageId);
        return messageRepository.findById(MessageId.of(messageId))
                .map(messageMapper::toDto);
    }

    /**
     * Retrieve a page of messages for a conversation, ordered newest-first.
     *
     * @param conversationId the conversation ID
     * @param page           zero-based page index
     * @param size           page size (number of messages)
     * @return list of message DTOs
     */
    @Transactional(readOnly = true)
    public List<MessageDto> getConversationHistory(String conversationId, int page, int size) {
        log.debug("Fetching history for conversation={} page={} size={}", conversationId, page, size);

        List<Message> messages = messageRepository.findByConversationId(
                ConversationId.of(conversationId), page, size);

        return messages.stream()
                .map(messageMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve undelivered messages for a recipient.
     * Used during inbox sync when a user comes back online.
     *
     * @param receiverId the recipient user ID
     * @return list of undelivered message DTOs
     */
    @Transactional(readOnly = true)
    public List<MessageDto> getUndeliveredMessages(String receiverId) {
        log.debug("Fetching undelivered messages for receiverId={}", receiverId);

        return messageRepository.findUndeliveredMessages(receiverId)
                .stream()
                .map(messageMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Count the total number of messages in a conversation.
     *
     * @param conversationId the conversation ID
     * @return message count
     */
    @Transactional(readOnly = true)
    public long countMessages(String conversationId) {
        return messageRepository.countByConversationId(ConversationId.of(conversationId));
    }

    /**
     * Count undelivered messages for a recipient (inbox badge count).
     *
     * @param receiverId the recipient user ID
     * @return number of undelivered messages
     */
    @Transactional(readOnly = true)
    public long countUndelivered(String receiverId) {
        return messageRepository.countUndeliveredMessages(receiverId);
    }
}

