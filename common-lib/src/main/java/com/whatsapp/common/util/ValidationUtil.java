package com.whatsapp.common.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Validation Utility Class
 *
 * Common validation operations.
 *
 * @author WhatsApp Clone Team
 */
public final class ValidationUtil {

    private ValidationUtil() {
        // Prevent instantiation
    }

    // Regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[1-9]\\d{1,14}$"  // E.164 format
    );

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._-]{3,30}$"
    );

    /**
     * Check if string is null or empty
     */
    public static boolean isEmpty(String str) {
        return StringUtils.isEmpty(str);
    }

    /**
     * Check if string is not null and not empty
     */
    public static boolean isNotEmpty(String str) {
        return StringUtils.isNotEmpty(str);
    }

    /**
     * Check if string is null, empty, or whitespace only
     */
    public static boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }

    /**
     * Check if string is not blank
     */
    public static boolean isNotBlank(String str) {
        return StringUtils.isNotBlank(str);
    }

    /**
     * Check if collection is null or empty
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Check if collection is not empty
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate phone number format (E.164)
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return isNotBlank(phoneNumber) && PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    /**
     * Validate username format
     */
    public static boolean isValidUsername(String username) {
        return isNotBlank(username) && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Validate string length
     */
    public static boolean isLengthValid(String str, int minLength, int maxLength) {
        if (str == null) return false;
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Require non-null
     */
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }

    /**
     * Require non-empty string
     */
    public static String requireNonEmpty(String str, String message) {
        if (isEmpty(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }

    /**
     * Require non-blank string
     */
    public static String requireNonBlank(String str, String message) {
        if (isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }

    /**
     * Require valid email
     */
    public static String requireValidEmail(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        return email;
    }

    /**
     * Require valid phone number
     */
    public static String requireValidPhoneNumber(String phoneNumber) {
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
        }
        return phoneNumber;
    }

    /**
     * Require valid username
     */
    public static String requireValidUsername(String username) {
        if (!isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid username format: " + username);
        }
        return username;
    }
}