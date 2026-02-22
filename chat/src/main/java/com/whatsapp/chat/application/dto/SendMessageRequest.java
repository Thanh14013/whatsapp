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

    @NotBlank(message = "Receiver ID is required")
    private String receiverId;

    @NotBlank(message = "Message content is required")
    @Size(max = 10000, message = "Message content cannot exceed 10000 characters")
    private String content;

    @NotNull(message = "Content type is required")
    private ContentType contentType;

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