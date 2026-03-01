package com.example.interhubdev.auth.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for JWT authentication.
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
class AuthProperties {

    /**
     * Secret key for signing JWT tokens.
     * Must be at least 256 bits (32 characters) for HS256.
     */
    private String secret;

    /**
     * Access token configuration.
     */
    private TokenConfig access = new TokenConfig();

    /**
     * Refresh token configuration.
     */
    private TokenConfig refresh = new TokenConfig();

    /**
     * Cookie configuration.
     */
    private CookieConfig cookie = new CookieConfig();

    /**
     * General rate limit (requests per client).
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * Login-specific rate limit (failed attempts per IP).
     */
    private LoginRateLimitConfig loginRateLimit = new LoginRateLimitConfig();

    /**
     * Security response headers.
     */
    private SecurityHeadersConfig securityHeaders = new SecurityHeadersConfig();

    /**
     * Whether to trust X-Forwarded-For for client IP (only when behind a trusted reverse proxy).
     * When false, client IP is taken from request.getRemoteAddr() only.
     */
    private boolean trustProxy = false;

    /**
     * CORS configuration. When allowedOrigins is non-empty, cross-origin requests from those origins are permitted.
     */
    private CorsConfig cors = new CorsConfig();

    /**
     * Base URL for the password reset page (used in reset email body).
     * User is directed to this URL to enter the OTP code.
     */
    private String passwordResetBaseUrl = "http://localhost:5173";

    @Getter
    @Setter
    public static class RateLimitConfig {
        /** Max requests per window per client. */
        private int maxRequestsPerSecond = 50;
        /** Window duration in milliseconds. */
        private long windowMs = 1000L;
        /** Cleanup interval: remove client keys older than this (ms). */
        private long cleanupAfterInactiveMs = 60_000L;
    }

    @Getter
    @Setter
    public static class LoginRateLimitConfig {
        /** Max failed login attempts per IP per window. */
        private int maxAttempts = 5;
        /** Window duration in minutes. */
        private int windowMinutes = 15;
    }

    @Getter
    @Setter
    public static class SecurityHeadersConfig {
        /** X-Frame-Options value, e.g. DENY or SAMEORIGIN. */
        private String frameOptions = "DENY";
        /** X-Content-Type-Options value. */
        private String contentTypeOptions = "nosniff";
        /** HSTS max-age in seconds; 0 = disabled. */
        private long hstsMaxAgeSeconds = 0;
        /** Whether to include subdomains in HSTS. */
        private boolean hstsIncludeSubdomains = true;
    }

    @Getter
    @Setter
    public static class TokenConfig {
        /**
         * Token expiration time in milliseconds.
         */
        private long expiration;

        /**
         * Cookie name for this token.
         */
        private String cookieName;
    }

    @Getter
    @Setter
    public static class CookieConfig {
        /**
         * Whether cookies should be secure (HTTPS only).
         * Should be true in production.
         */
        private boolean secure = false;

        /**
         * SameSite attribute for cookies.
         * Strict recommended for CSRF protection.
         */
        private String sameSite = "Strict";

        /**
         * Cookie domain. Empty for current domain.
         */
        private String domain = "";

        /**
         * Cookie path.
         */
        private String path = "/api";
    }

    @Getter
    @Setter
    public static class CorsConfig {
        /** Allowed origins for CORS (e.g. https://app.example.com). Empty = no CORS; do not use * with credentials. */
        private List<String> allowedOrigins = new ArrayList<>();
    }
}
