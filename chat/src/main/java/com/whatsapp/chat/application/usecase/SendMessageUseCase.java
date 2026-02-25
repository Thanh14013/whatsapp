package com.whatsapp.chat.application.usecase;

import com.whatsapp.chat.application.dto.MessageDto;
import com.whatsapp.chat.application.service.ChatApplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Send Message Use Case
 *
 * Encapsulates the "Send Message" user story as a dedicated use-case object.
 * Delegates orchestration to {@link ChatApplicationService} while keeping
 * the interface layer thin.
 *
 * Use-case steps:
 *  1. Validate sender is a participant in the conversation
 *  2. Persist the message (MongoDB)
 *  3. Update conversation metadata (PostgreSQL)
 *  4. Invalidate relevant cache keys (Redis)
 *  5. Publish MESSAGE_SENT event (RabbitMQ)
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendMessageUseCase {

    private final ChatApplicationService chatApplicationService;

    /**
     * Execute the send-message use case.
     *
     * @param senderId       ID of the user sending the message
     * @param receiverId     ID of the intended recipient
     * @param conversationId ID of the conversation
     * @param content        Text or media URL
     * @param contentType    Content type (TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT)
     * @param replyToId      Optional – ID of the message being replied to
     * @return the persisted {@link MessageDto}
     */
    public MessageDto execute(
            String senderId,
            String receiverId,
            String conversationId,
            String content,
            String contentType,
            String replyToId) {

        log.info("[SendMessageUseCase] senderId={} conversationId={}", senderId, conversationId);

        com.whatsapp.chat.application.dto.SendMessageRequest request =
                com.whatsapp.chat.application.dto.SendMessageRequest.builder()
                        .senderId(senderId)
                        .receiverId(receiverId)
                        .conversationId(conversationId)
                        .content(content)
                        .contentType(com.whatsapp.chat.application.dto.SendMessageRequest.ContentType
                                .valueOf(contentType != null ? contentType : "TEXT"))
                        .replyToMessageId(replyToId)
                        .build();

        MessageDto result = chatApplicationService.sendMessage(request);

        log.info("[SendMessageUseCase] Message sent successfully: {}", result.getId());
        return result;
    }
}

