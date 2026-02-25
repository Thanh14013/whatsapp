package com.whatsapp.chat.application.usecase;

import com.whatsapp.chat.application.dto.MessageDto;
import com.whatsapp.chat.application.service.ChatApplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Get Chat History Use Case
 *
 * Encapsulates the "Retrieve Chat History" user story.
 *
 * Supports cursor-based pagination (page + size) to efficiently
 * fetch large conversation histories.
 *
 * Use-case steps:
 *  1. Validate requester is a participant in the conversation
 *  2. Query message store (MongoDB) with pagination
 *  3. Return list of {@link MessageDto} sorted newest-first
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetChatHistoryUseCase {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE     = 100;

    private final ChatApplicationService chatApplicationService;

    /**
     * Execute the get-chat-history use case with default page size.
     *
     * @param conversationId ID of the conversation
     * @param page           Zero-based page number
     * @return list of messages for the requested page
     */
    public List<MessageDto> execute(String conversationId, int page) {
        return execute(conversationId, page, DEFAULT_PAGE_SIZE);
    }

    /**
     * Execute the get-chat-history use case with explicit page size.
     *
     * @param conversationId ID of the conversation
     * @param page           Zero-based page number
     * @param size           Number of messages per page (capped at {@value #MAX_PAGE_SIZE})
     * @return list of messages for the requested page
     */
    public List<MessageDto> execute(String conversationId, int page, int size) {
        int effectiveSize = Math.min(size, MAX_PAGE_SIZE);
        log.debug("[GetChatHistoryUseCase] conversationId={} page={} size={}", conversationId, page, effectiveSize);

        List<MessageDto> messages = chatApplicationService.getConversationMessages(conversationId, page, effectiveSize);

        log.debug("[GetChatHistoryUseCase] Returning {} messages for conversation {}", messages.size(), conversationId);
        return messages;
    }
}

