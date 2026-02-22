package com.whatsapp.chat.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Conversation DTO
 *
 * Data Transfer Object for conversation information.
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    private String id;
    private String type;
    private String name;
    private String description;
    private String avatarUrl;
    private List<String> participantIds;
    private String lastMessageId;
    private Instant lastMessageTimestamp;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
