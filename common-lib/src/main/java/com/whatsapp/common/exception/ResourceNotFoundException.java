package com.whatsapp.common.exception;

/**
 * Resource Not Found Exception
 *
 * Exception when a requested resource is not found.
 * HTTP Status: 404 (Not Found)
 *
 * Examples:
 * - User not found
 * - Message not found
 * - Conversation not found
 *
 * @author WhatsApp Clone Team
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", 404);
    }

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s not found: %s", resourceType, resourceId),
                "RESOURCE_NOT_FOUND", 404);
    }

    public ResourceNotFoundException(String message, Object metadata) {
        super(message, "RESOURCE_NOT_FOUND", 404, metadata);
    }
}