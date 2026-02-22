package com.whatsapp.chat.application.mapper;

import com.whatsapp.chat.application.dto.ConversationDto;
import com.whatsapp.chat.domain.model.Conversation;
import com.whatsapp.chat.domain.model.vo.Participant;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Conversation Mapper
 *
 * Maps between Conversation domain model and ConversationDto.
 *
 * @author WhatsApp Clone Team
 */
@Component
public class ConversationMapper {

    /**
     * Convert Conversation domain model to ConversationDto
     */
    public ConversationDto toDto(Conversation conversation) {
        if (conversation == null) {
            return null;
        }

        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId().getValue());
        dto.setType(conversation.getType().name());
        dto.setName(conversation.getName());
        dto.setDescription(conversation.getDescription());
        dto.setAvatarUrl(conversation.getAvatarUrl());
        dto.setParticipantIds(conversation.getParticipants().stream()
                .map(Participant::getUserId)
                .collect(Collectors.toList()));
        dto.setLastMessageId(conversation.getLastMessageId());
        dto.setLastMessageTimestamp(conversation.getLastMessageTimestamp());
        dto.setActive(conversation.isActive());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());

        return dto;
    }
}
