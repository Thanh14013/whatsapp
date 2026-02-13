package com.whatsapp.common.exception;

import lombok.Getter;

/**
 * Base Exception
 *
 * Base class for all custom exceptions in the application.
 * Provides consistent exception handling across microservices.
 *
 * @author WhatsApp Clone Team
 */
@Getter
public class BaseException extends RuntimeException {

    /**
     * Error code for categorization
     */
    private final String errorCode;

    /**
     * HTTP status code
     */
    private final int httpStatus;

    /**
     * Additional metadata
     */
    private final Object metadata;

    public BaseException(String message) {
        super(message);
        this.errorCode = "INTERNAL_ERROR";
        this.httpStatus = 500;
        this.metadata = null;
    }

    public BaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 500;
        this.metadata = null;
    }

    public BaseException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.metadata = null;
    }

    public BaseException(String message, String errorCode, int httpStatus, Object metadata) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.metadata = metadata;
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INTERNAL_ERROR";
        this.httpStatus = 500;
        this.metadata = null;
    }

    public BaseException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = 500;
        this.metadata = null;
    }
}