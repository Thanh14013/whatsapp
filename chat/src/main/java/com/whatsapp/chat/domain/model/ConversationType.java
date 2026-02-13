package com.whatsapp.chat.domain.model;

/**
 * ConversationType Enum
 *
 * Represents the type of conversation.
 *
 * @author WhatsApp Clone Team
 */
public enum ConversationType {

    /**
     * One-to-one private conversation between two users
     */
    ONE_TO_ONE("One-to-One"),

    /**
     * Group conversation with multiple participants
     */
    GROUP("Group"),

    /**
     * Broadcast message to multiple recipients (future feature)
     */
    BROADCAST("Broadcast");

    private final String displayName;

    ConversationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
