package com.example.interhubdev.auth.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

/**
 * Auth module error factory. All errors exposed to API go through {@link com.example.interhubdev.error}.
 */
final class AuthErrors {

    private AuthErrors() {
    }

    /** Invalid or expired password reset OTP code. */
    public static final String CODE_RESET_INVALID_OR_EXPIRED = "AUTH_RESET_CODE_INVALID_OR_EXPIRED";

    public static AppException invalidOrExpiredResetCode() {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_RESET_INVALID_OR_EXPIRED,
                "Неверный или истёкший код. Запросите новый код восстановления пароля.");
    }
}
