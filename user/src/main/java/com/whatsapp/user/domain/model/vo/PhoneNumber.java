package com.whatsapp.user.domain.model.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * PhoneNumber Value Object
 *
 * Represents a valid phone number in international format.
 * Immutable value object with validation.
 *
 * Format: +[country code][number]
 * Example: +84912345678, +1234567890
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneNumber {

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+[1-9]\\d{1,14}$"  // E.164 format
    );

    private String value;

    /**
     * Create PhoneNumber from string value
     */
    public static PhoneNumber of(String value) {
        Objects.requireNonNull(value, "Phone number cannot be null");

        String trimmedValue = value.trim();

        if (trimmedValue.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        // Add + prefix if not present
        if (!trimmedValue.startsWith("+")) {
            trimmedValue = "+" + trimmedValue;
        }

        if (!PHONE_PATTERN.matcher(trimmedValue).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + value + ". Must be in format +[country code][number]");
        }

        return new PhoneNumber(trimmedValue);
    }

    /**
     * Get country code from phone number
     */
    public String getCountryCode() {
        // Extract country code (1-3 digits after +)
        if (value.length() >= 4) {
            String possibleCode = value.substring(1, 4);
            if (possibleCode.matches("\\d{3}")) {
                return possibleCode;
            }
        }
        if (value.length() >= 3) {
            String possibleCode = value.substring(1, 3);
            if (possibleCode.matches("\\d{2}")) {
                return possibleCode;
            }
        }
        return value.substring(1, 2);
    }

    /**
     * Get phone number without country code
     */
    public String getNumberWithoutCountryCode() {
        String countryCode = getCountryCode();
        return value.substring(countryCode.length() + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhoneNumber that = (PhoneNumber) o;
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