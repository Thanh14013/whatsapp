package com.whatsapp.chat.application.mapper;

import com.whatsapp.chat.application.dto.MessageDto;
import com.whatsapp.chat.domain.model.Message;

import org.springframework.stereotype.Component;

/**
 * Message Mapper
 *
 * Maps between Message domain model and MessageDto.
 *
 * @author WhatsApp Clone Team
 */
@Component
public class MessageMapper {

    /**
     * Convert Message domain model to MessageDto
     */
    public MessageDto toDto(Message message) {
        if (message == null) {
            return null;
        }

        MessageDto dto = new MessageDto();
        dto.setId(message.getId().getValue());
        dto.setConversationId(message.getConversationId().getValue());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setContentType(message.getContent().getType().name());
        dto.setContent(message.getContent().getText());
        dto.setMediaUrl(message.getContent().getMediaUrl());
        dto.setStatus(message.getStatus().name());
        dto.setSentAt(message.getSentAt());
        dto.setDeliveredAt(message.getDeliveredAt());
        dto.setReadAt(message.getReadAt());
        dto.setReplyToMessageId(message.getReplyToMessageId());
        dto.setDeleted(message.isDeleted());
        dto.setCreatedAt(message.getCreatedAt());

        return dto;
    }
}
