package com.whatsapp.chat.domain.model;

/**
 * MessageType Enum
 *
 * Represents the type of message content.
 *
 * @author WhatsApp Clone Team
 */
public enum MessageType {

    /**
     * Plain text message
     */
    TEXT("Text"),

    /**
     * Image attachment
     */
    IMAGE("Image"),

    /**
     * Video attachment
     */
    VIDEO("Video"),

    /**
     * Audio/voice message
     */
    AUDIO("Audio"),

    /**
     * Document/file attachment
     */
    DOCUMENT("Document"),

    /**
     * Location coordinates
     */
    LOCATION("Location"),

    /**
     * Contact card
     */
    CONTACT("Contact"),

    /**
     * Sticker
     */
    STICKER("Sticker");

    private final String displayName;

    MessageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
