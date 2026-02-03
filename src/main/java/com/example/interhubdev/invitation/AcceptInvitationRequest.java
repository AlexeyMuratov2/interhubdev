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
            throw new IllegalArgumentException("Укажите ссылку приглашения (токен).");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Пароль должен быть не менее 8 символов.");
        }
    }
}
