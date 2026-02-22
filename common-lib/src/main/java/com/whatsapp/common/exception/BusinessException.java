package com.whatsapp.common.exception;

/**
 * Business Exception
 *
 * Exception for business logic violations.
 * HTTP Status: 400 (Bad Request)
 *
 * Examples:
 * - User already exists
 * - Invalid operation
 * - Business rule violation
 *
 * @author WhatsApp Clone Team
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(message, "BUSINESS_ERROR", 400);
    }

    public BusinessException(String message, String errorCode) {
        super(message, errorCode, 400);
    }

    public BusinessException(String message, String errorCode, Object metadata) {
        super(message, errorCode, 400, metadata);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, "BUSINESS_ERROR", cause);
    }
}