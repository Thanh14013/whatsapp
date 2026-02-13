package com.whatsapp.chat.domain.service;

import com.whatsapp.chat.domain.model.Message;
import com.whatsapp.chat.domain.model.vo.ConversationId;
import com.whatsapp.chat.domain.model.vo.MessageContent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Message Domain Service
 *
 * Encapsulates domain logic that doesn't naturally fit within a single
 * aggregate.
 * Handles message creation and validation rules.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDomainService {

    /**
     * Create a new message with business rule validation
     */
    public Message createMessage(
            ConversationId conversationId,
            String senderId,
            String receiverId,
            MessageContent content,
            String replyToMessageId) {

        log.debug("Creating new message from {} to {}", senderId, receiverId);

        // Validate sender and receiver are different
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        // Create message
        Message message = Message.create(
                conversationId,
                senderId,
                receiverId,
                content,
                replyToMessageId);

        log.debug("Message created successfully: {}", message.getId());
        return message;
    }

    /**
     * Validate message content length
     */
    public void validateMessageContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        if (content.length() > 10000) {
            throw new IllegalArgumentException("Message content exceeds maximum length of 10000 characters");
        }
    }
}
