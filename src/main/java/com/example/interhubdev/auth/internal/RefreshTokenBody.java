package com.example.interhubdev.auth.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Optional JSON body for {@code POST /api/auth/refresh} and {@code POST /api/auth/logout}
 * when refresh cookies are not available (Bearer-style clients).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RefreshTokenBody(String refreshToken) {
}
