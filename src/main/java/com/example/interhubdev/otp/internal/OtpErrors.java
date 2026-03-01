package com.example.interhubdev.otp.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

/**
 * OTP module error factory. All errors exposed to API go through {@link com.example.interhubdev.error}.
 */
final class OtpErrors {

    private OtpErrors() {
    }

    /** Too many requests: new OTP requested before rate limit window passed. */
    public static final String CODE_RATE_LIMIT = "OTP_RATE_LIMIT";

    /** Too many failed verification attempts for this purpose+subject. */
    public static final String CODE_TOO_MANY_ATTEMPTS = "OTP_TOO_MANY_ATTEMPTS";

    /** Invalid arguments (e.g. blank purpose/subject). */
    public static final String CODE_INVALID_ARGUMENT = "OTP_INVALID_ARGUMENT";

    /** Redis/store unavailable. */
    public static final String CODE_SERVICE_UNAVAILABLE = "OTP_SERVICE_UNAVAILABLE";

    public static AppException serviceUnavailable(String message) {
        return Errors.of(HttpStatus.SERVICE_UNAVAILABLE, CODE_SERVICE_UNAVAILABLE, message);
    }

    public static AppException rateLimit(String message) {
        return Errors.of(HttpStatus.TOO_MANY_REQUESTS, CODE_RATE_LIMIT, message);
    }

    public static AppException tooManyAttempts(String message) {
        return Errors.of(HttpStatus.TOO_MANY_REQUESTS, CODE_TOO_MANY_ATTEMPTS, message);
    }

    public static AppException invalidArgument(String message) {
        return Errors.badRequest(message);
    }
}
