package com.whatsapp.user.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * UserProfile Value Object
 *
 * Represents user profile information.
 * Immutable value object following DDD principles.
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserProfile {

    private String displayName;
    private String bio;
    private String avatarUrl;
    private String phoneNumber;
    private String statusMessage;

    /**
     * Create default profile
     */
    public static UserProfile createDefault() {
        return new UserProfile(
                "User",
                "",
                null,
                null,
                "Hey there! I am using WhatsApp Clone"
        );
    }

    /**
     * Create profile with display name
     */
    public static UserProfile create(String displayName, String bio, String avatarUrl, String phoneNumber) {
        validateDisplayName(displayName);

        return new UserProfile(
                displayName,
                bio != null ? bio : "",
                avatarUrl,
                phoneNumber,
                "Hey there! I am using WhatsApp Clone"
        );
    }

    /**
     * Update display name
     */
    public UserProfile withDisplayName(String newDisplayName) {
        validateDisplayName(newDisplayName);
        return new UserProfile(newDisplayName, this.bio, this.avatarUrl, this.phoneNumber, this.statusMessage);
    }

    /**
     * Update bio
     */
    public UserProfile withBio(String newBio) {
        return new UserProfile(this.displayName, newBio, this.avatarUrl, this.phoneNumber, this.statusMessage);
    }

    /**
     * Update avatar URL
     */
    public UserProfile withAvatarUrl(String newAvatarUrl) {
        return new UserProfile(this.displayName, this.bio, newAvatarUrl, this.phoneNumber, this.statusMessage);
    }

    /**
     * Update status message
     */
    public UserProfile withStatusMessage(String newStatusMessage) {
        return new UserProfile(this.displayName, this.bio, this.avatarUrl, this.phoneNumber, newStatusMessage);
    }

    /**
     * Validate display name
     */
    private static void validateDisplayName(String displayName) {
        Objects.requireNonNull(displayName, "Display name cannot be null");

        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Display name cannot be empty");
        }

        if (displayName.length() > 50) {
            throw new IllegalArgumentException("Display name cannot exceed 50 characters");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfile that = (UserProfile) o;
        return Objects.equals(displayName, that.displayName) &&
                Objects.equals(bio, that.bio) &&
                Objects.equals(avatarUrl, that.avatarUrl) &&
                Objects.equals(phoneNumber, that.phoneNumber) &&
                Objects.equals(statusMessage, that.statusMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, bio, avatarUrl, phoneNumber, statusMessage);
    }
}