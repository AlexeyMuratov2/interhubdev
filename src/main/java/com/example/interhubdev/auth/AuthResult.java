package com.example.interhubdev.auth;

import com.example.interhubdev.user.Role;

import java.util.UUID;

/**
 * Authentication result returned after successful login.
 * Note: Actual tokens are set in HttpOnly cookies, not returned in response body.
 */
public record AuthResult(
        UUID userId,
        String email,
        Role role,
        String fullName,
        String message
) {
    /**
     * Create success result.
     */
    public static AuthResult success(UUID userId, String email, Role role, String fullName) {
        return new AuthResult(userId, email, role, fullName, "Login successful");
    }
}
