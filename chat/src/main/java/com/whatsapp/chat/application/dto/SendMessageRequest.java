package com.whatsapp.chat.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Send Message Request DTO
 *
 * Data Transfer Object for sending a message.
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /** ID of the user sending the message (resolved from auth principal when available) */
    private String senderId;

    @NotBlank(message = "Receiver ID is required")
    private String receiverId;

    @NotBlank(message = "Conversation ID is required")
    private String conversationId;

    @NotBlank(message = "Message content is required")
    @Size(max = 10000, message = "Message content cannot exceed 10000 characters")
    private String content;

    @NotNull(message = "Content type is required")
    private ContentType contentType;

    /** Optional: ID of the message being replied to */
    private String replyToMessageId;

    /**
     * Content Type Enum
     */
    public enum ContentType {
        TEXT,
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT
    }
}