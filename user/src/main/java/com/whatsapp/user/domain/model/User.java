package com.whatsapp.user.domain.model;

import com.whatsapp.user.domain.model.vo.Email;
import com.whatsapp.user.domain.model.vo.PhoneNumber;
import com.whatsapp.user.domain.model.vo.UserId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/**
 * User Domain Model (Aggregate Root)
 *
 * Represents a user in the WhatsApp Clone system.
 * This is a rich domain model containing business logic and invariants.
 *
 * Aggregate Root: User controls access to UserProfile and UserStatus
 *
 * Business Rules:
 * - Username must be unique
 * - Email must be valid and unique
 * - Phone number must be valid and unique
 * - Password must be encrypted
 * - User can be activated/deactivated
 * - Last seen timestamp is updated on activity
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    private UserId id;
    private String username;
    private Email email;
    private PhoneNumber phoneNumber;
    private String passwordHash;
    private UserProfile profile;
    private UserStatus status;
    private boolean active;
    private boolean emailVerified;
    private boolean phoneVerified;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Factory method to create a new User
     */
    public static User create(
            String username,
            Email email,
            PhoneNumber phoneNumber,
            String passwordHash,
            UserProfile profile) {

        validateUsername(username);
        Objects.requireNonNull(email, "Email cannot be null");
        Objects.requireNonNull(phoneNumber, "Phone number cannot be null");
        Objects.requireNonNull(passwordHash, "Password hash cannot be null");

        Instant now = Instant.now();

        return new User(
                UserId.generate(),
                username,
                email,
                phoneNumber,
                passwordHash,
                profile != null ? profile : UserProfile.createDefault(),
                UserStatus.OFFLINE,
                true,
                false,
                false,
                now,
                now
        );
    }

    /**
     * Update user profile
     */
    public void updateProfile(UserProfile newProfile) {
        Objects.requireNonNull(newProfile, "Profile cannot be null");
        this.profile = newProfile;
        this.updatedAt = Instant.now();
    }

    /**
     * Change user status (online/offline/away)
     */
    public void changeStatus(UserStatus newStatus) {
        Objects.requireNonNull(newStatus, "Status cannot be null");
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Update password
     */
    public void changePassword(String newPasswordHash) {
        Objects.requireNonNull(newPasswordHash, "Password hash cannot be null");
        if (newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }

    /**
     * Verify email
     */
    public void verifyEmail() {
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }

    /**
     * Verify phone number
     */
    public void verifyPhone() {
        this.phoneVerified = true;
        this.updatedAt = Instant.now();
    }

    /**
     * Activate user account
     */
    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    /**
     * Deactivate user account
     */
    public void deactivate() {
        this.active = false;
        this.status = UserStatus.OFFLINE;
        this.updatedAt = Instant.now();
    }

    /**
     * Check if user is online
     */
    public boolean isOnline() {
        return this.active && this.status == UserStatus.ONLINE;
    }

    /**
     * Update last seen timestamp
     */
    public void updateLastSeen() {
        this.updatedAt = Instant.now();
    }

    /**
     * Validate username
     */
    private static void validateUsername(String username) {
        Objects.requireNonNull(username, "Username cannot be null");

        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (username.length() < 3 || username.length() > 30) {
            throw new IllegalArgumentException("Username must be between 3 and 30 characters");
        }

        if (!username.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, dots, underscores, and hyphens");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}