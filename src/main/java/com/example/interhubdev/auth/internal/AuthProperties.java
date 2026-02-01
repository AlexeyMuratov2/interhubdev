package com.example.interhubdev.auth.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
}
