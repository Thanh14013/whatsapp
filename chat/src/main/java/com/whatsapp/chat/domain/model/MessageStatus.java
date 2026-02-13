package com.whatsapp.chat.domain.model;

/**
 * MessageStatus Enum
 *
 * Represents the delivery status of a message.
 *
 * Status Flow:
 * SENT → DELIVERED → READ
 *
 * @author WhatsApp Clone Team
 */
public enum MessageStatus {

    /**
     * Message sent from sender, not yet delivered to receiver
     */
    SENT("Sent"),

    /**
     * Message delivered to receiver's device
     */
    DELIVERED("Delivered"),

    /**
     * Message read by receiver
     */
    READ("Read");

    private final String displayName;

    MessageStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}