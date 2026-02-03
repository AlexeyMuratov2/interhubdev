package com.example.interhubdev.invitation;

import com.example.interhubdev.user.Role;

import java.util.List;
import java.util.UUID;

/**
 * Result of token validation (GET /api/invitations/validate?token=...).
 * Use {@link #code()} to distinguish outcome; {@link #valid()} and {@link #tokenRegenerated()} for quick checks.
 */
public record TokenValidationResult(
    boolean valid,
    UUID invitationId,
    UUID userId,
    String email,
    List<Role> roles,
    String firstName,
    String lastName,
    String error,
    boolean tokenRegenerated,
    String code
) {

    /** Token not found or already used. */
    public static final String CODE_TOKEN_INVALID = "INVITATION_TOKEN_INVALID";
    /** Invitation validity period (e.g. 90 days) has expired. */
    public static final String CODE_INVITATION_EXPIRED = "INVITATION_EXPIRED";
    /** Invitation is no longer acceptable (accepted, cancelled or expired status). */
    public static final String CODE_INVITATION_NOT_ACCEPTABLE = "INVITATION_NOT_ACCEPTABLE";
    /** Token expired; new invitation email was sent to the user. */
    public static final String CODE_TOKEN_EXPIRED_EMAIL_RESENT = "INVITATION_TOKEN_EXPIRED_EMAIL_RESENT";

    /**
     * Creates a successful validation result (token valid, user can proceed to accept).
     */
    public static TokenValidationResult success(
            UUID invitationId,
            UUID userId,
            String email,
            List<Role> roles,
            String firstName,
            String lastName,
            boolean tokenRegenerated
    ) {
        return new TokenValidationResult(
            true, invitationId, userId, email,
            roles != null ? List.copyOf(roles) : List.of(),
            firstName, lastName, null, tokenRegenerated, null
        );
    }

    /**
     * Creates a failed validation result with a machine-readable code.
     */
    public static TokenValidationResult failure(String code, String error) {
        return new TokenValidationResult(
            false, null, null, null, null, null, null, error, false, code
        );
    }

    /**
     * Token expired; new token was created and a new invitation email was sent.
     * Frontend should show a distinct message (e.g. "Check your email for a new link").
     */
    public static TokenValidationResult tokenRegeneratedAndSent(String email) {
        return new TokenValidationResult(
            false, null, null, email, null, null, null,
            "Ссылка истекла. На вашу почту отправлено новое приглашение.",
            true, CODE_TOKEN_EXPIRED_EMAIL_RESENT
        );
    }
}
