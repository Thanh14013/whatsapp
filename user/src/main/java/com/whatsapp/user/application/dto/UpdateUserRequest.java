package com.whatsapp.user.application.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update User Request DTO
 *
 * Data Transfer Object for user update requests.
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 1, max = 50, message = "Display name must be between 1 and 50 characters")
    private String displayName;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    private String avatarUrl;

    @Size(max = 255, message = "Status message cannot exceed 255 characters")
    private String statusMessage;
}