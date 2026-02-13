package com.whatsapp.user.infrastructure.persistence.entity;

import com.whatsapp.user.domain.model.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * User JPA Entity
 *
 * Persistence model for User.
 * Maps to 'users' table in PostgreSQL database.
 *
 * @author WhatsApp Clone Team
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_phone_number", columnList = "phone_number"),
        @Index(name = "idx_users_status", columnList = "status"),
        @Index(name = "idx_users_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false, length = 255)
    private String id;

    @Column(name = "username", nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // Profile fields
    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "status_message", length = 255)
    private String statusMessage;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified;

    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified;

    // Timestamps
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (active == null) {
            active = true;
        }
        if (emailVerified == null) {
            emailVerified = false;
        }
        if (phoneVerified == null) {
            phoneVerified = false;
        }
        if (status == null) {
            status = UserStatus.OFFLINE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}