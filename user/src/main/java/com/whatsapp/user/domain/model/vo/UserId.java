package com.whatsapp.user.domain.model.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

/**
 * UserId Value Object
 *
 * Represents a unique identifier for a User.
 * Immutable value object following DDD principles.
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserId {

    private String value;

    /**
     * Generate new UserId
     */
    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }

    /**
     * Create UserId from existing value
     */
    public static UserId of(String value) {
        Objects.requireNonNull(value, "UserId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("UserId value cannot be empty");
        }
        return new UserId(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserId userId = (UserId) o;
        return Objects.equals(value, userId.value);
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