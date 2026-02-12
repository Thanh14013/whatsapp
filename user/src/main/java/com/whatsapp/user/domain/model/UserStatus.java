package com.whatsapp.user.domain.model;

/**
 * UserStatus Enum
 *
 * Represents the current status of a user.
 *
 * @author WhatsApp Clone Team
 */
public enum UserStatus {

    /**
     * User is currently online and active
     */
    ONLINE("Online"),

    /**
     * User is offline
     */
    OFFLINE("Offline"),

    /**
     * User is away/idle
     */
    AWAY("Away"),

    /**
     * User is busy/do not disturb
     */
    BUSY("Busy");

    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}