package com.example.interhubdev.account.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Account-specific error codes and user-facing messages.
 * All messages are suitable for direct display to the user.
 */
public final class AccountErrors {

    private AccountErrors() {
    }

    public static final String CODE_USER_NOT_FOUND = "ACCOUNT_USER_NOT_FOUND";
    public static final String CODE_FORBIDDEN = "ACCOUNT_FORBIDDEN";
    public static final String CODE_CANNOT_EDIT_EMAIL = "ACCOUNT_CANNOT_EDIT_EMAIL";
    public static final String CODE_CANNOT_DELETE_SELF = "ACCOUNT_CANNOT_DELETE_SELF";
    public static final String CODE_ONLY_SUPER_ADMIN_CAN_DELETE_SUPER_ADMIN = "ACCOUNT_ONLY_SUPER_ADMIN_CAN_DELETE_SUPER_ADMIN";

    public static AppException userNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_USER_NOT_FOUND,
                "Пользователь не найден: " + id);
    }

    public static AppException forbiddenManageUsers() {
        return Errors.forbidden(
                "Недостаточно прав для управления пользователями.");
    }

    public static AppException cannotEditEmail() {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_CANNOT_EDIT_EMAIL,
                "Изменение email недоступно.");
    }

    public static AppException cannotDeleteSelf() {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_CANNOT_DELETE_SELF,
                "Нельзя удалить свой аккаунт этим способом.");
    }

    public static AppException onlySuperAdminCanDeleteSuperAdmin() {
        return Errors.of(HttpStatus.FORBIDDEN, CODE_ONLY_SUPER_ADMIN_CAN_DELETE_SUPER_ADMIN,
                "Удалить супер-администратора может только другой супер-администратор.");
    }
}
