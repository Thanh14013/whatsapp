package com.whatsapp.user.application.dto;

import com.whatsapp.user.domain.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * User Data Transfer Object
 *
 * Represents user information for external communication.
 * Does not expose sensitive information like password hash.
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private String id;
    private String username;
    private String email;
    private String phoneNumber;

    // Profile
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String statusMessage;

    // Status
    private UserStatus status;
    private boolean active;
    private boolean emailVerified;
    private boolean phoneVerified;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastSeenAt;
}