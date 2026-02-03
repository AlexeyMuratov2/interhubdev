package com.example.interhubdev.auth;

import com.example.interhubdev.user.Role;

import java.util.List;
import java.util.UUID;

/**
 * Authentication result returned after successful login.
 * Note: Actual tokens are set in HttpOnly cookies, not returned in response body.
 */
public record AuthResult(
        UUID userId,
        String email,
        List<Role> roles,
        String fullName,
        String message
) {
    /**
     * Create success result.
     */
    public static AuthResult success(UUID userId, String email, List<Role> roles, String fullName) {
        return new AuthResult(userId, email, roles != null ? List.copyOf(roles) : List.of(), fullName, "Login successful");
    }
}
