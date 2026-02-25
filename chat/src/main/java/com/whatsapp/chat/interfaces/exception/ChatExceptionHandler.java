package com.whatsapp.chat.interfaces.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chat Exception Handler
 *
 * Global exception handler for the Chat Service REST API.
 * Uses RFC 7807 Problem Detail format via Spring's {@link ProblemDetail}.
 *
 * Handled exceptions:
 *  - {@link IllegalArgumentException}             → 400 Bad Request
 *  - {@link IllegalStateException}                → 409 Conflict
 *  - {@link MethodArgumentNotValidException}      → 422 Unprocessable Entity
 *  - {@link MissingServletRequestParameterException} → 400 Bad Request
 *  - {@link MethodArgumentTypeMismatchException}  → 400 Bad Request
 *  - {@link Exception} (fallback)                 → 500 Internal Server Error
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@RestControllerAdvice
public class ChatExceptionHandler {

    private static final URI TYPE_BASE = URI.create("https://api.whatsapp-clone.com/errors/");

    // ---------------------------------------------------------------
    // 400 Bad Request
    // ---------------------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(TYPE_BASE.resolve("bad-request"));
        problem.setTitle("Bad Request");
        problem.setDetail(ex.getMessage());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(TYPE_BASE.resolve("missing-parameter"));
        problem.setTitle("Missing Request Parameter");
        problem.setDetail("Required parameter '" + ex.getParameterName() + "' is missing");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(TYPE_BASE.resolve("type-mismatch"));
        problem.setTitle("Invalid Parameter Type");
        problem.setDetail("Parameter '" + ex.getName() + "' has an invalid value");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    // ---------------------------------------------------------------
    // 409 Conflict
    // ---------------------------------------------------------------

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setType(TYPE_BASE.resolve("conflict"));
        problem.setTitle("Conflict");
        problem.setDetail(ex.getMessage());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    // ---------------------------------------------------------------
    // 422 Unprocessable Entity  (Bean Validation failures)
    // ---------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a));   // keep first error per field

        log.warn("Validation failed: {}", fieldErrors);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setType(TYPE_BASE.resolve("validation-error"));
        problem.setTitle("Validation Failed");
        problem.setDetail("One or more fields failed validation");
        problem.setProperty("errors", fieldErrors);
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    // ---------------------------------------------------------------
    // 500 Internal Server Error (fallback)
    // ---------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(TYPE_BASE.resolve("internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred. Please try again later.");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.internalServerError().body(problem);
    }
}

