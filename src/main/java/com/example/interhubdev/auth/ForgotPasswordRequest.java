package com.example.interhubdev.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to request a password reset email.
 * User receives an OTP by email if an active account exists.
 */
public record ForgotPasswordRequest(
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    String email
) {}
