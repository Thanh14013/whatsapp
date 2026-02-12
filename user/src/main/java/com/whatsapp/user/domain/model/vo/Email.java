package com.whatsapp.user.domain.model.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Email Value Object
 *
 * Represents a valid email address.
 * Immutable value object with validation.
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Email {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private String value;

    /**
     * Create Email from string value
     */
    public static Email of(String value) {
        Objects.requireNonNull(value, "Email cannot be null");

        String trimmedValue = value.trim().toLowerCase();

        if (trimmedValue.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (!EMAIL_PATTERN.matcher(trimmedValue).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }

        return new Email(trimmedValue);
    }

    /**
     * Get domain part of email
     */
    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }

    /**
     * Get local part of email (before @)
     */
    public String getLocalPart() {
        return value.substring(0, value.indexOf('@'));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(value, email.value);
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