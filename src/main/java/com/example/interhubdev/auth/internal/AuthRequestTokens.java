package com.example.interhubdev.auth.internal;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Extracts bearer access tokens from HTTP requests (Authorization header).
 */
final class AuthRequestTokens {

    private AuthRequestTokens() {
    }

    /**
     * Reads {@code Authorization: Bearer <token>} (case-insensitive scheme).
     */
    static Optional<String> bearerAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        final String prefix = "Bearer ";
        if (header.length() <= prefix.length() || !header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return Optional.empty();
        }
        String token = header.substring(prefix.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
