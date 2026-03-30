package com.example.interhubdev.auth;

import com.example.interhubdev.user.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/**
 * Authentication result returned after successful login or refresh.
 * Tokens are normally set as HttpOnly cookies; when the client sends {@code X-Auth-Tokens: json},
 * {@code accessToken} and {@code refreshToken} are also included for Bearer-based clients (e.g. Telegram WebView).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResult(
        UUID userId,
        String email,
        List<Role> roles,
        String fullName,
        String message,
        String accessToken,
        String refreshToken
) {
    /**
     * Create success result (cookie-only; no tokens in JSON body).
     */
    public static AuthResult success(UUID userId, String email, List<Role> roles, String fullName) {
        return success(userId, email, roles, fullName, "Login successful", null, null);
    }

    public static AuthResult success(
            UUID userId,
            String email,
            List<Role> roles,
            String fullName,
            String message,
            String accessToken,
            String refreshToken) {
        return new AuthResult(
                userId,
                email,
                roles != null ? List.copyOf(roles) : List.of(),
                fullName,
                message,
                accessToken,
                refreshToken);
    }
}
