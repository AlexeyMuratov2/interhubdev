package com.example.interhubdev.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to reset password using OTP code from email.
 */
public record ResetPasswordRequest(
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    String email,

    @NotBlank(message = "Код подтверждения обязателен")
    String code,

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, message = "Пароль должен быть не менее 8 символов")
    String newPassword
) {}
