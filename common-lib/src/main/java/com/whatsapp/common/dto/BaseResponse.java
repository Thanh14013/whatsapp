package com.whatsapp.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Base Response DTO
 *
 * Standard response wrapper for all API endpoints.
 * Provides consistent response structure across all microservices.
 *
 * Structure:
 * - success: Boolean indicating success/failure
 * - message: Human-readable message
 * - data: Response payload (generic type)
 * - timestamp: Response timestamp
 * - error: Error details (only present on failure)
 *
 * Usage Examples:
 *
 * Success Response:
 * {
 *   "success": true,
 *   "message": "User created successfully",
 *   "data": { ... },
 *   "timestamp": "2026-02-13T10:00:00Z"
 * }
 *
 * Error Response:
 * {
 *   "success": false,
 *   "message": "Validation failed",
 *   "error": { "code": "VALIDATION_ERROR", "details": {...} },
 *   "timestamp": "2026-02-13T10:00:00Z"
 * }
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    /**
     * Success flag
     */
    private Boolean success;

    /**
     * Human-readable message
     */
    private String message;

    /**
     * Response payload
     */
    private T data;

    /**
     * Response timestamp
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Error details (only present on failure)
     */
    private ErrorDetails error;

    /**
     * Create success response with data
     */
    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create success response with data and message
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return BaseResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create success response with message only
     */
    public static <T> BaseResponse<T> success(String message) {
        return BaseResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create error response
     */
    public static <T> BaseResponse<T> error(String message) {
        return BaseResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create error response with details
     */
    public static <T> BaseResponse<T> error(String message, ErrorDetails errorDetails) {
        return BaseResponse.<T>builder()
                .success(false)
                .message(message)
                .error(errorDetails)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Error Details inner class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private String code;
        private String details;
        private Object metadata;
    }
}