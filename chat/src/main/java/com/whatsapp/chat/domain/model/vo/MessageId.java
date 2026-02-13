package com.whatsapp.chat.domain.model.vo;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * MessageId Value Object
 *
 * Represents a unique identifier for a Message using Snowflake algorithm.
 * Snowflake generates distributed unique IDs that are:
 * - Time-ordered (sortable by creation time)
 * - Globally unique across all instances
 * - 64-bit long integer
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageId {

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    private String value;

    /**
     * Generate new MessageId using Snowflake algorithm
     */
    public static MessageId generate() {
        long id = SNOWFLAKE.nextId();
        return new MessageId(String.valueOf(id));
    }

    /**
     * Create MessageId from existing value
     */
    public static MessageId of(String value) {
        Objects.requireNonNull(value, "MessageId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("MessageId value cannot be empty");
        }
        return new MessageId(value);
    }

    /**
     * Get the timestamp from Snowflake ID
     */
    public long getTimestamp() {
        long id = Long.parseLong(value);
        // Extract timestamp from Snowflake ID (first 41 bits)
        return (id >> 22) + 1288834974657L; // Snowflake epoch: 2010-11-04
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageId messageId = (MessageId) o;
        return Objects.equals(value, messageId.value);
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