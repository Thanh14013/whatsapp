package com.whatsapp.chat.domain.service;

import com.whatsapp.chat.domain.model.Message;
import com.whatsapp.chat.domain.model.MessageStatus;
import com.whatsapp.chat.domain.model.vo.MessageId;
import com.whatsapp.chat.domain.repository.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Delivery Tracking Service (Domain Service)
 *
 * Responsible for tracking and updating message delivery status.
 * Encapsulates domain logic that spans multiple entities or requires
 * coordination across the delivery lifecycle.
 *
 * Delivery Lifecycle:
 *   SENT  →  DELIVERED  →  READ
 *
 * Responsibilities:
 * - Bulk-mark messages as delivered when a user comes online
 * - Validate status transitions (guard against illegal state changes)
 * - Provide delivery stats for a conversation
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class DeliveryTrackingService {

    private final MessageRepository messageRepository;

    /**
     * Mark all undelivered messages for a recipient as DELIVERED.
     * Called when the user's WebSocket session is established.
     *
     * @param receiverId ID of the user who just came online
     * @return number of messages marked as delivered
     */
    public int deliverPendingMessages(String receiverId) {
        log.info("Delivering pending messages for user: {}", receiverId);

        List<Message> undelivered = messageRepository.findUndeliveredMessages(receiverId);

        int count = 0;
        for (Message message : undelivered) {
            if (message.getStatus() == MessageStatus.SENT) {
                message.markAsDelivered();
                messageRepository.save(message);
                count++;
            }
        }

        log.info("Delivered {} pending messages to user: {}", count, receiverId);
        return count;
    }

    /**
     * Validate that a status transition is legal.
     *
     * Allowed transitions:
     *   SENT → DELIVERED
     *   SENT → READ        (skip delivered, e.g. web client)
     *   DELIVERED → READ
     *
     * @param current desired current status before transition
     * @param next    desired next status
     * @throws IllegalStateException if the transition is not allowed
     */
    public void validateStatusTransition(MessageStatus current, MessageStatus next) {
        boolean valid = switch (next) {
            case DELIVERED -> current == MessageStatus.SENT;
            case READ      -> current == MessageStatus.SENT || current == MessageStatus.DELIVERED;
            default        -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    String.format("Invalid message status transition: %s → %s", current, next));
        }
    }

    /**
     * Check whether a specific message has been delivered.
     *
     * @param messageId the message ID to check
     * @return {@code true} if the message status is DELIVERED or READ
     */
    public boolean isDelivered(String messageId) {
        return messageRepository.findById(MessageId.of(messageId))
                .map(Message::isDelivered)
                .orElse(false);
    }

    /**
     * Check whether a specific message has been read.
     *
     * @param messageId the message ID to check
     * @return {@code true} if the message status is READ
     */
    public boolean isRead(String messageId) {
        return messageRepository.findById(MessageId.of(messageId))
                .map(Message::isRead)
                .orElse(false);
    }

    /**
     * Get the count of undelivered messages for a user (inbox badge).
     *
     * @param receiverId the user ID
     * @return count of undelivered messages
     */
    public long getUndeliveredCount(String receiverId) {
        return messageRepository.countUndeliveredMessages(receiverId);
    }
}

