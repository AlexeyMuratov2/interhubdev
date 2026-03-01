package com.example.interhubdev.auth;

/**
 * Response for forgot-password endpoint. Same message regardless of whether email was sent.
 */
public record ForgotPasswordResponse(String message) {}
