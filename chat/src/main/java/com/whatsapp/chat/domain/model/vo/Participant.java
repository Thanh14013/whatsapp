package com.whatsapp.chat.domain.model.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Participant Value Object
 *
 * Represents a participant in a conversation.
 * Immutable value object with user reference.
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Participant {

    private String userId;
    private String displayName;
    private boolean admin;

    /**
     * Create regular participant
     */
    public static Participant create(String userId, String displayName) {
        validateUserId(userId);
        validateDisplayName(displayName);
        return new Participant(userId, displayName, false);
    }

    /**
     * Create admin participant
     */
    public static Participant createAdmin(String userId, String displayName) {
        validateUserId(userId);
        validateDisplayName(displayName);
        return new Participant(userId, displayName, true);
    }

    /**
     * Create participant with existing data
     */
    public static Participant of(String userId, String displayName, boolean admin) {
        validateUserId(userId);
        validateDisplayName(displayName);
        return new Participant(userId, displayName, admin);
    }

    /**
     * Promote to admin
     */
    public Participant promoteToAdmin() {
        return new Participant(this.userId, this.displayName, true);
    }

    /**
     * Demote from admin
     */
    public Participant demoteFromAdmin() {
        return new Participant(this.userId, this.displayName, false);
    }

    /**
     * Validate user ID
     */
    private static void validateUserId(String userId) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        if (userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
    }

    /**
     * Validate display name
     */
    private static void validateDisplayName(String displayName) {
        Objects.requireNonNull(displayName, "Display name cannot be null");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Display name cannot be empty");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Participant that = (Participant) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "Participant{userId='" + userId + "', displayName='" + displayName + "', admin=" + admin + "}";
    }
}
