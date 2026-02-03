package com.example.interhubdev.invitation.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.invitation.InvitationStatus;
import org.springframework.http.HttpStatus;

/**
 * Invitation-specific error codes and user-facing messages for the frontend.
 * All messages are suitable for direct display to the user.
 */
public final class InvitationErrors {

    private InvitationErrors() {
    }

    // ——— Validation / token (400) ———
    public static final String CODE_TOKEN_INVALID = "INVITATION_TOKEN_INVALID";
    public static final String CODE_TOKEN_EXPIRED = "INVITATION_TOKEN_EXPIRED";
    public static final String CODE_ROLE_REQUIRED = "INVITATION_ROLE_REQUIRED";

    // ——— Conflict (409) ———
    public static final String CODE_ALREADY_ACCEPTED = "INVITATION_ALREADY_ACCEPTED";
    public static final String CODE_ALREADY_ACTIVATED = "INVITATION_ALREADY_ACTIVATED";
    public static final String CODE_NOT_ACCEPTABLE = "INVITATION_NOT_ACCEPTABLE";
    public static final String CODE_USER_EXISTS = "INVITATION_USER_EXISTS";
    public static final String CODE_RESEND_NOT_ALLOWED = "INVITATION_RESEND_NOT_ALLOWED";
    public static final String CODE_CANCEL_NOT_ALLOWED = "INVITATION_CANCEL_NOT_ALLOWED";

    // ——— Not found (404) ———
    public static final String CODE_INVITATION_NOT_FOUND = "INVITATION_NOT_FOUND";
    public static final String CODE_INVITER_NOT_FOUND = "INVITATION_INVITER_NOT_FOUND";
    public static final String CODE_USER_NOT_FOUND = "INVITATION_USER_NOT_FOUND";

    public static AppException tokenInvalid() {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_TOKEN_INVALID,
                "Ссылка приглашения недействительна или уже использована.");
    }

    public static AppException tokenExpired() {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_TOKEN_EXPIRED,
                "Срок действия ссылки истёк. Запросите новое приглашение у администратора.");
    }

    public static AppException roleRequired() {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_ROLE_REQUIRED,
                "Укажите роль приглашаемого пользователя.");
    }

    public static AppException invitationNotFound(Object id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_INVITATION_NOT_FOUND,
                "Приглашение не найдено: " + id);
    }

    public static AppException inviterNotFound(Object id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_INVITER_NOT_FOUND,
                "Приглашающий не найден.");
    }

    public static AppException userNotFound(Object id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_USER_NOT_FOUND,
                "Пользователь не найден.");
    }

    public static AppException alreadyActivated() {
        return Errors.of(HttpStatus.CONFLICT, CODE_ALREADY_ACTIVATED,
                "Аккаунт уже активирован. Войдите, используя пароль.");
    }

    public static AppException invitationNotAcceptable(InvitationStatus status) {
        return Errors.of(HttpStatus.CONFLICT, CODE_NOT_ACCEPTABLE,
                "Приглашение недоступно для активации (статус: " + status + ").");
    }

    public static AppException userAlreadyExists(String email) {
        return Errors.of(HttpStatus.CONFLICT, CODE_USER_EXISTS,
                "Пользователь с email " + email + " уже зарегистрирован или ожидает активации.");
    }

    public static AppException resendNotAllowed(InvitationStatus status) {
        return Errors.of(HttpStatus.CONFLICT, CODE_RESEND_NOT_ALLOWED,
                "Повторная отправка невозможна: приглашение уже принято, отменено или истекло (статус: " + status + ").");
    }

    public static AppException cancelNotAllowed() {
        return Errors.of(HttpStatus.CONFLICT, CODE_CANCEL_NOT_ALLOWED,
                "Нельзя отменить уже принятое приглашение.");
    }

    public static AppException cannotInviteRole(String inviterRoles, String targetRole) {
        return Errors.forbidden(
                "Недостаточно прав для приглашения с ролью " + targetRole + ". Ваши роли: " + inviterRoles + ".");
    }
}
