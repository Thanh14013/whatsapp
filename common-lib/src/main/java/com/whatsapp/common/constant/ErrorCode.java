package com.whatsapp.common.constant;

/**
 * Error Code Constants
 *
 * Centralized error codes for consistent error handling.
 *
 * Categories:
 * - VALIDATION_*: Input validation errors
 * - AUTH_*: Authentication/Authorization errors
 * - USER_*: User-related errors
 * - MESSAGE_*: Messaging errors
 * - SYSTEM_*: System/Infrastructure errors
 *
 * @author WhatsApp Clone Team
 */
public final class ErrorCode {

    private ErrorCode() {
        // Prevent instantiation
    }

    // ===== Validation Errors =====
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String MISSING_REQUIRED_FIELD = "MISSING_REQUIRED_FIELD";

    // ===== Authentication/Authorization Errors =====
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";

    // ===== User Errors =====
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    public static final String USERNAME_TAKEN = "USERNAME_TAKEN";
    public static final String EMAIL_TAKEN = "EMAIL_TAKEN";
    public static final String PHONE_TAKEN = "PHONE_TAKEN";
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";
    public static final String WEAK_PASSWORD = "WEAK_PASSWORD";

    // ===== Message Errors =====
    public static final String MESSAGE_NOT_FOUND = "MESSAGE_NOT_FOUND";
    public static final String MESSAGE_TOO_LONG = "MESSAGE_TOO_LONG";
    public static final String CANNOT_DELETE_MESSAGE = "CANNOT_DELETE_MESSAGE";
    public static final String CONVERSATION_NOT_FOUND = "CONVERSATION_NOT_FOUND";
    public static final String INVALID_RECEIVER = "INVALID_RECEIVER";

    // ===== Resource Errors =====
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String RESOURCE_ALREADY_EXISTS = "RESOURCE_ALREADY_EXISTS";
    public static final String RESOURCE_CONFLICT = "RESOURCE_CONFLICT";

    // ===== System Errors =====
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String CACHE_ERROR = "CACHE_ERROR";
    public static final String MESSAGING_ERROR = "MESSAGING_ERROR";

    // ===== Rate Limiting =====
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";
}