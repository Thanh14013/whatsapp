package com.whatsapp.chat.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Create Conversation Request DTO
 *
 * Data Transfer Object for creating a conversation.
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotBlank(message = "Type is required")
    private String type; // ONE_TO_ONE or GROUP

    // For ONE_TO_ONE conversations
    private String participant1Id;
    private String participant1Name;
    private String participant2Id;
    private String participant2Name;

    // For GROUP conversations
    private String name;
    private String description;
    private String creatorId;
    private String creatorName;
    private List<ParticipantDto> additionalParticipants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDto {
        private String userId;
        private String displayName;
    }
}
