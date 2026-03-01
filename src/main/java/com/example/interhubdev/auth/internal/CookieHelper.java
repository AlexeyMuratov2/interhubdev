package com.example.interhubdev.auth.internal;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Helper for managing authentication cookies.
 */
@Component
@RequiredArgsConstructor
class CookieHelper {

    private final AuthProperties authProperties;

    /**
     * Set access token cookie.
     */
    public void setAccessTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        ResponseCookie cookie = buildCookie(
                authProperties.getAccess().getCookieName(),
                token,
                maxAgeSeconds,
                authProperties.getCookie().getPath()
        );
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Set refresh token cookie.
     * Uses more restrictive path for refresh token.
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        ResponseCookie cookie = buildCookie(
                authProperties.getRefresh().getCookieName(),
                token,
                maxAgeSeconds,
                authProperties.getCookie().getPath() + "/auth"
        );
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Clear all auth cookies (for logout).
     */
    public void clearAuthCookies(HttpServletResponse response) {
        // Clear access token
        ResponseCookie accessCookie = buildCookie(
                authProperties.getAccess().getCookieName(),
                "",
                0,
                authProperties.getCookie().getPath()
        );
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // Clear refresh token
        ResponseCookie refreshCookie = buildCookie(
                authProperties.getRefresh().getCookieName(),
                "",
                0,
                authProperties.getCookie().getPath() + "/auth"
        );
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    /**
     * Extract access token from request cookies.
     */
    public Optional<String> getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, authProperties.getAccess().getCookieName());
    }

    /**
     * Extract refresh token from request cookies.
     */
    public Optional<String> getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, authProperties.getRefresh().getCookieName());
    }

    /**
     * Get client IP address. When trust-proxy is enabled, uses first value of X-Forwarded-For;
     * otherwise uses request.getRemoteAddr() only (safe when not behind a trusted reverse proxy).
     */
    public String getClientIp(HttpServletRequest request) {
        if (!authProperties.isTrustProxy()) {
            return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    /**
     * Get user agent from request.
     */
    public String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 500) {
            return userAgent.substring(0, 500);
        }
        return userAgent;
    }

    private ResponseCookie buildCookie(String name, String value, int maxAgeSeconds, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(authProperties.getCookie().isSecure())
                .path(path)
                .maxAge(maxAgeSeconds)
                .sameSite(authProperties.getCookie().getSameSite());

        String domain = authProperties.getCookie().getDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        return builder.build();
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }
}
