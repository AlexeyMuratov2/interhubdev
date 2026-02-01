package com.example.interhubdev.error;

import org.springframework.http.HttpStatus;

/**
 * Convenient API for throwing application errors from any module.
 * <p>
 * Usage examples:
 * <pre>{@code
 * // Common cases
 * throw Errors.badRequest("Invalid token");
 * throw Errors.notFound("Invitation not found: " + id);
 * throw Errors.conflict("Invitation cannot be accepted in current status");
 * throw Errors.unauthorized("Authentication required");
 * throw Errors.forbidden("Insufficient permissions");
 *
 * // Custom status and code
 * throw Errors.of(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", "Custom validation failed");
 *
 * // With details (e.g. field errors)
 * throw Errors.badRequest("Validation failed", Map.of("email", "must be valid email"));
 * }</pre>
 */
public final class Errors {

    private Errors() {
    }

    /** 400 Bad Request */
    public static AppException badRequest(String message) {
        return new AppException("BAD_REQUEST", HttpStatus.BAD_REQUEST, message);
    }

    /** 400 Bad Request with details */
    public static AppException badRequest(String message, Object details) {
        return new AppException("BAD_REQUEST", HttpStatus.BAD_REQUEST, message, details);
    }

    /** 401 Unauthorized */
    public static AppException unauthorized(String message) {
        return new AppException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, message);
    }

    /** 403 Forbidden */
    public static AppException forbidden(String message) {
        return new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, message);
    }

    /** 404 Not Found */
    public static AppException notFound(String message) {
        return new AppException("NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }

    /** 409 Conflict */
    public static AppException conflict(String message) {
        return new AppException("CONFLICT", HttpStatus.CONFLICT, message);
    }

    /** 422 Unprocessable Entity */
    public static AppException unprocessable(String message) {
        return new AppException("UNPROCESSABLE_ENTITY", HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    /** Custom status, code and message */
    public static AppException of(HttpStatus status, String code, String message) {
        return new AppException(code, status, message);
    }

    /** Custom status, code, message and details */
    public static AppException of(HttpStatus status, String code, String message, Object details) {
        return new AppException(code, status, message, details);
    }
}
