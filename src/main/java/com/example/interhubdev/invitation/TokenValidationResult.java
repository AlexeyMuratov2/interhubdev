package com.example.interhubdev.invitation;

import com.example.interhubdev.user.Role;

import java.util.UUID;

/**
 * Result of token validation.
 */
public record TokenValidationResult(
    boolean valid,
    UUID invitationId,
    UUID userId,
    String email,
    Role role,
    String firstName,
    String lastName,
    String error,
    boolean tokenRegenerated
) {
    /**
     * Creates a successful validation result.
     */
    public static TokenValidationResult success(
            UUID invitationId, 
            UUID userId, 
            String email, 
            Role role,
            String firstName,
            String lastName,
            boolean tokenRegenerated
    ) {
        return new TokenValidationResult(
            true, invitationId, userId, email, role, firstName, lastName, null, tokenRegenerated
        );
    }

    /**
     * Creates a failed validation result.
     */
    public static TokenValidationResult failure(String error) {
        return new TokenValidationResult(false, null, null, null, null, null, null, error, false);
    }

    /**
     * Creates a result indicating token was regenerated and new email sent.
     */
    public static TokenValidationResult tokenRegeneratedAndSent(String email) {
        return new TokenValidationResult(
            false, null, null, email, null, null, null, 
            "Token expired. A new invitation email has been sent.", 
            true
        );
    }
}
