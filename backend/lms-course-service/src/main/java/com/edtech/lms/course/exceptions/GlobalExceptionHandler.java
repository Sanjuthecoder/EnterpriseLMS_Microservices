package com.edtech.lms.course.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — Centralized exception handling for lms-course-service.
 *
 * <p>All domain and system exceptions are caught here. No controller may have its own
 * try-catch blocks (architectural rule from Spring Boot standards).
 *
 * <p>Logging levels:
 * <ul>
 *   <li>WARN — recoverable business rule violations (not found, conflict, bad input).</li>
 *   <li>ERROR — unexpected system errors (Kafka failures, DB errors, AI ingestion failure).</li>
 * </ul>
 *
 * <p>PII policy: Employee IDs and company IDs are never logged at WARN/ERROR level.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // DOMAIN EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            final ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            final UnauthorizedException ex) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    // =========================================================================
    // BUSINESS RULE VIOLATIONS
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            final IllegalArgumentException ex) {
        log.warn("Bad request — illegal argument: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            final IllegalStateException ex) {
        log.warn("Conflict — illegal state: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurity(
            final SecurityException ex) {
        log.warn("Forbidden operation: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    // =========================================================================
    // VALIDATION
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            final MethodArgumentNotValidException ex) {
        final String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", details);
        return build(HttpStatus.BAD_REQUEST, "Validation Failed", details);
    }

    // =========================================================================
    // AI QUIZ INGESTION (Kafka consumer errors)
    // =========================================================================

    /**
     * Catches RuntimeExceptions thrown by {@code AiQuizKafkaConsumer} when JSON parsing
     * fails or a batch save fails. These propagate to let Kafka retry or route to DLQ.
     *
     * <p>This handler only fires if the exception somehow escapes to the HTTP layer
     * (e.g., via the admin trigger endpoint). For Kafka listener failures, Kafka
     * itself handles retries independently of this handler.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            final RuntimeException ex) {
        log.error("Runtime error in course service: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected runtime error occurred. Please try again or check logs.");
    }

    // =========================================================================
    // CATCH-ALL
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            final Exception ex) {
        log.error("Unexpected error in course service: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again.");
    }

    // =========================================================================
    // HELPER
    // =========================================================================

    /**
     * Builds a consistent error response map with timestamp, status, error type, and message.
     *
     * @param status  HTTP status
     * @param error   short error label
     * @param message detailed message (no PII)
     * @return ResponseEntity with the error map
     */
    private ResponseEntity<Map<String, Object>> build(
            final HttpStatus status, final String error, final String message) {
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
