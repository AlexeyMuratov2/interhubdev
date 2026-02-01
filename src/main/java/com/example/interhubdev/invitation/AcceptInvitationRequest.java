package com.example.interhubdev.invitation;

/**
 * Request object for accepting an invitation.
 */
public record AcceptInvitationRequest(
    String token,
    String password
) {
    public AcceptInvitationRequest {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token is required");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }
    }
}
