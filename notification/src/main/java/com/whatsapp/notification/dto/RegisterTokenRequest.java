package com.whatsapp.notification.dto;

import com.whatsapp.notification.domain.model.DeviceToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Register Token Request DTO
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterTokenRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Device token is required")
    private String token;

    @NotNull(message = "Platform is required")
    private DeviceToken.DevicePlatform platform;

    private String deviceName;
}