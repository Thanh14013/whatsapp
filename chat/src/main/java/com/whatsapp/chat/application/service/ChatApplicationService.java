package com.whatsapp.chat.application.service;

import com.whatsapp.chat.application.dto.MessageDto;
import com.whatsapp.chat.application.dto.SendMessageRequest;
import com.whatsapp.chat.application.mapper.MessageMapper;
import com.whatsapp.chat.domain.model.Message;
import com.whatsapp.chat.domain.model.vo.ConversationId;
import com.whatsapp.chat.domain.model.vo.MessageContent;
import com.whatsapp.chat.domain.model.vo.MessageId;
import com.whatsapp.chat.domain.repository.ConversationRepository;
import com.whatsapp.chat.domain.repository.MessageRepository;
import com.whatsapp.chat.domain.service.MessageDomainService;
import com.whatsapp.chat.infrastructure.cache.InboxCacheService;
import com.whatsapp.chat.infrastructure.messaging.MessageEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat Application Service
 *
 * Orchestrates chat-related use cases including message sending, delivery
 * tracking,
 * and message history retrieval.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class ChatApplicationService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MessageDomainService messageDomainService;
    private final InboxCacheService cacheService;
    private final MessageEventPublisher eventPublisher;
    private final MessageMapper messageMapper;

    /**
     * Send a new message
     */
    @Transactional
    public MessageDto sendMessage(SendMessageRequest request) {
        log.info("Sending message from {} to {} in conversation {}",
                request.getSenderId(), request.getReceiverId(), request.getConversationId());

        // Validate conversation exists
        ConversationId conversationId = ConversationId.of(request.getConversationId());
        var conversation = conversationRepository.findById(conversationId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Conversation not found: " + request.getConversationId()));

        // Validate sender is participant
        if (!conversation.isParticipant(request.getSenderId())) {
            throw new IllegalArgumentException("Sender is not a participant in this conversation");
        }

        // Create message content
        MessageContent content = MessageContent.text(request.getContent());

        // Create message using domain service
        Message message = messageDomainService.createMessage(
                conversationId,
                request.getSenderId(),
                request.getReceiverId(),
                content,
                request.getReplyToMessageId());

        // Save message
        Message savedMessage = messageRepository.save(message);

        // Update conversation
        conversation.updateLastMessage(savedMessage.getId().getValue(), savedMessage.getSentAt());
        conversation.incrementUnreadCount(request.getReceiverId());
        conversationRepository.save(conversation);

        // Invalidate cache
        cacheService.evictConversation(request.getConversationId());

        // Publish event
        eventPublisher.publishMessageSent(savedMessage);

        log.info("Message sent successfully: {}", savedMessage.getId());

        return messageMapper.toDto(savedMessage);
    }

    /**
     * Mark message as delivered
     */
    @Transactional
    public MessageDto markAsDelivered(String messageId, String userId) {
        log.debug("Marking message as delivered: {}", messageId);

        Message message = messageRepository.findById(MessageId.of(messageId))
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        // Validate user is receiver
        if (!message.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("User is not the receiver of this message");
        }

        message.markAsDelivered();
        Message updatedMessage = messageRepository.save(message);

        // Publish event
        eventPublisher.publishMessageDelivered(updatedMessage);

        log.info("Message marked as delivered: {}", messageId);

        return messageMapper.toDto(updatedMessage);
    }

    /**
     * Mark message as read
     */
    @Transactional
    public MessageDto markAsRead(String messageId, String userId) {
        log.debug("Marking message as read: {}", messageId);

        Message message = messageRepository.findById(MessageId.of(messageId))
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        // Validate user is receiver
        if (!message.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("User is not the receiver of this message");
        }

        message.markAsRead();
        Message updatedMessage = messageRepository.save(message);

        // Update conversation unread count
        var conversation = conversationRepository.findById(message.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        conversation.resetUnreadCount(userId);
        conversationRepository.save(conversation);

        // Invalidate cache
        cacheService.evictConversation(conversation.getId().getValue());

        // Publish event
        eventPublisher.publishMessageRead(updatedMessage);

        log.info("Message marked as read: {}", messageId);

        return messageMapper.toDto(updatedMessage);
    }

    /**
     * Get message by ID
     */
    @Transactional(readOnly = true)
    public MessageDto getMessage(String messageId) {
        log.debug("Getting message by ID: {}", messageId);

        Message message = messageRepository.findById(MessageId.of(messageId))
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        return messageMapper.toDto(message);
    }

    /**
     * Get messages by conversation with pagination
     */
    @Transactional(readOnly = true)
    public List<MessageDto> getConversationMessages(String conversationId, int page, int size) {
        log.debug("Getting messages for conversation: {}, page: {}, size: {}", conversationId, page, size);

        List<Message> messages = messageRepository.findByConversationId(
                ConversationId.of(conversationId),
                page,
                size);

        return messages.stream()
                .map(messageMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Delete message (soft delete)
     */
    @Transactional
    public void deleteMessage(String messageId, String userId) {
        log.info("Deleting message: {}", messageId);

        Message message = messageRepository.findById(MessageId.of(messageId))
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        // Validate user is sender
        if (!message.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("Only sender can delete message");
        }

        message.delete(userId);
        messageRepository.save(message);

        log.info("Message deleted: {}", messageId);
    }
}
