package com.whatsapp.chat.domain.model.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

/**
 * ConversationId Value Object
 *
 * Represents a unique identifier for a Conversation.
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConversationId {

    private String value;

    /**
     * Generate new ConversationId
     */
    public static ConversationId generate() {
        return new ConversationId(UUID.randomUUID().toString());
    }

    /**
     * Create ConversationId from existing value
     */
    public static ConversationId of(String value) {
        Objects.requireNonNull(value, "ConversationId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ConversationId value cannot be empty");
        }
        return new ConversationId(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationId that = (ConversationId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}