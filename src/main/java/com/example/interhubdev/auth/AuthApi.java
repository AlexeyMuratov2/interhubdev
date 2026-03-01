package com.example.interhubdev.auth;

import com.example.interhubdev.user.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Public API for the Auth module.
 * Handles authentication, token management, and session operations.
 */
public interface AuthApi {

    /**
     * Authenticate user and create session.
     * Sets access and refresh tokens as HttpOnly cookies.
     *
     * @param email    user email
     * @param password plain text password
     * @param request  HTTP request (for IP, user agent)
     * @param response HTTP response (for setting cookies)
     * @return auth result with user info
     * @throws AuthenticationException if credentials are invalid or user cannot login
     */
    AuthResult login(String email, String password, HttpServletRequest request, HttpServletResponse response);

    /**
     * Refresh access token using refresh token from cookie.
     *
     * @param request  HTTP request (contains refresh token cookie)
     * @param response HTTP response (for setting new cookies)
     * @return auth result with user info
     * @throws AuthenticationException if refresh token is invalid or expired
     */
    AuthResult refresh(HttpServletRequest request, HttpServletResponse response);

    /**
     * Logout user - revoke current refresh token.
     *
     * @param request  HTTP request (contains refresh token cookie)
     * @param response HTTP response (for clearing cookies)
     */
    void logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * Logout from all devices - revoke all user's refresh tokens.
     *
     * @param userId   user ID
     * @param response HTTP response (for clearing cookies)
     */
    void logoutAll(UUID userId, HttpServletResponse response);

    /**
     * Revoke all refresh tokens for a user (e.g. before account deletion). Does not clear cookies.
     *
     * @param userId user ID
     */
    void revokeAllTokensForUser(UUID userId);

    /**
     * Get current authenticated user from request.
     * Validates access token from cookie.
     *
     * @param request HTTP request
     * @return user info if authenticated, empty otherwise
     */
    Optional<UserDto> getCurrentUser(HttpServletRequest request);

    /**
     * Request password reset: if a user with the given email exists and is ACTIVE,
     * creates an OTP and sends it by email. Response is always the same (no user enumeration).
     *
     * @param email user email (normalized)
     * @throws com.example.interhubdev.error.AppException from OtpApi e.g. rate limit exceeded
     */
    void requestPasswordReset(String email);

    /**
     * Reset password using OTP: verifies the code, sets new password, revokes all sessions.
     *
     * @param email       user email (same as used in requestPasswordReset)
     * @param code        OTP code from email
     * @param newPassword new password (min 8 chars)
     * @throws com.example.interhubdev.error.AppException if code invalid/expired or user not found
     */
    void resetPassword(String email, String code, String newPassword);

    /**
     * Authentication exception for login failures.
     */
    class AuthenticationException extends RuntimeException {
        private final AuthErrorCode errorCode;

        public AuthenticationException(AuthErrorCode errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public AuthErrorCode getErrorCode() {
            return errorCode;
        }
    }

    /**
     * Error codes for authentication failures.
     */
    enum AuthErrorCode {
        INVALID_CREDENTIALS,
        USER_NOT_ACTIVE,
        USER_DISABLED,
        TOKEN_EXPIRED,
        TOKEN_INVALID,
        USER_NOT_FOUND,
        TOO_MANY_REQUESTS
    }
}
