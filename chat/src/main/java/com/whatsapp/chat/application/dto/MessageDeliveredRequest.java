package com.whatsapp.chat.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message Delivered Request DTO
 *
 * Data Transfer Object used when a client acknowledges
 * that a message has been delivered to their device.
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDeliveredRequest {

    /**
     * ID of the message that was delivered
     */
    @NotBlank(message = "Message ID is required")
    private String messageId;

    /**
     * ID of the user who received the message
     */
    @NotBlank(message = "User ID is required")
    private String userId;
}

