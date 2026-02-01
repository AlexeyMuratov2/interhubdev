package com.example.interhubdev.error;

import org.springframework.http.HttpStatus;

/**
 * Base application exception with HTTP status and error code.
 * Thrown from services/controllers and converted to {@link ErrorResponse} by the global handler.
 */
public class AppException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Object details;

    public AppException(String code, HttpStatus status, String message) {
        this(code, status, message, null);
    }

    public AppException(String code, HttpStatus status, String message, Object details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }

    public AppException(String code, HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.details = null;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getDetails() {
        return details;
    }
}
