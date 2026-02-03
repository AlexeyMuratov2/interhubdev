package com.example.interhubdev.error.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global REST exception handler.
 * Converts exceptions to unified {@link ErrorResponse} and appropriate HTTP status.
 */
@RestControllerAdvice
@Slf4j
class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.debug("Conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Неверное значение", (a, b) -> a + "; " + b));
        log.debug("Validation failed: {}", fieldErrors);
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of("VALIDATION_FAILED", "Ошибка проверки данных. Проверьте заполненные поля.", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Неверный формат данных запроса.";
        Throwable cause = ex.getCause();
        if (cause instanceof IllegalArgumentException iae && iae.getMessage() != null && !iae.getMessage().isBlank()) {
            message = iae.getMessage();
        } else {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("problem: ")) {
                int idx = msg.indexOf("problem: ") + "problem: ".length();
                message = msg.substring(idx).split("\n")[0].trim();
            }
        }
        log.debug("Request body not readable: {}", message);
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of("VALIDATION_FAILED", message));
    }

    @ExceptionHandler(AuthApi.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthApi.AuthenticationException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case INVALID_CREDENTIALS, TOKEN_INVALID, TOKEN_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case USER_NOT_ACTIVE, USER_DISABLED -> HttpStatus.FORBIDDEN;
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(ex.getErrorCode().name(), ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String code = status != null ? statusToCode(status) : ex.getStatusCode().toString();
        String message = ex.getReason();
        if (message == null || message.isBlank()) {
            message = status != null ? statusToDefaultMessage(status) : ex.getStatusCode().toString();
        }
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ErrorResponse.of(code, message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Недостаточно прав для выполнения этого действия.";
        }
        log.debug("Access denied: {}", message);
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("FORBIDDEN", message));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Требуется вход в систему.";
        }
        log.debug("Authentication failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("UNAUTHORIZED", message));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.debug("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", "Метод или адрес не найдены."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.debug("Method not allowed: {}", ex.getMethod());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of("METHOD_NOT_ALLOWED", "Для этого адреса не поддерживается указанный метод запроса."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        log.debug("Unsupported media type: {}", ex.getContentType());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of("UNSUPPORTED_MEDIA_TYPE", "Неподдерживаемый тип содержимого запроса."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Произошла ошибка. Попробуйте позже."));
    }

    private static String statusToCode(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 409 -> "CONFLICT";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 422 -> "UNPROCESSABLE_ENTITY";
            case 500 -> "INTERNAL_ERROR";
            default -> status.name().replace(" ", "_");
        };
    }

    private static String statusToDefaultMessage(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> "Неверный запрос.";
            case 401 -> "Требуется вход в систему.";
            case 403 -> "Недостаточно прав для выполнения этого действия.";
            case 404 -> "Ресурс не найден.";
            case 405 -> "Для этого адреса не поддерживается указанный метод.";
            case 409 -> "Действие невозможно из-за текущего состояния данных.";
            case 415 -> "Неподдерживаемый тип содержимого.";
            case 422 -> "Запрос не может быть обработан.";
            case 500 -> "Произошла ошибка. Попробуйте позже.";
            default -> status.getReasonPhrase();
        };
    }
}
