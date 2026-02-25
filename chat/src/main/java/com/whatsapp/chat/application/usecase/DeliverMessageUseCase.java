package com.whatsapp.chat.application.usecase;

import com.whatsapp.chat.application.dto.MessageDto;
import com.whatsapp.chat.application.service.ChatApplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Deliver Message Use Case
 *
 * Encapsulates the "Mark Message as Delivered" user story.
 *
 * This use case is triggered when the recipient's device confirms
 * it has received the message payload (e.g., after a WebSocket
 * ACK or a REST acknowledgement call).
 *
 * Use-case steps:
 *  1. Lookup message by ID
 *  2. Validate caller is the designated receiver
 *  3. Transition status SENT → DELIVERED
 *  4. Persist updated message
 *  5. Publish MESSAGE_DELIVERED event (RabbitMQ)
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliverMessageUseCase {

    private final ChatApplicationService chatApplicationService;

    /**
     * Execute the deliver-message use case.
     *
     * @param messageId ID of the message to mark as delivered
     * @param userId    ID of the user acknowledging delivery (must be the receiver)
     * @return updated {@link MessageDto}
     */
    public MessageDto execute(String messageId, String userId) {
        log.info("[DeliverMessageUseCase] messageId={} userId={}", messageId, userId);

        MessageDto result = chatApplicationService.markAsDelivered(messageId, userId);

        log.info("[DeliverMessageUseCase] Message marked as delivered: {}", messageId);
        return result;
    }
}

