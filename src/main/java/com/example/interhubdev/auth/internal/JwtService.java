package com.example.interhubdev.auth.internal;

import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for JWT token generation and validation.
 */
@Service
@Slf4j
class JwtService {

    private static final String DEFAULT_SECRET = "default-secret-key-for-development-only-change-in-production-min-32-chars";

    private final AuthProperties authProperties;
    private final org.springframework.core.env.Environment environment;
    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    JwtService(AuthProperties authProperties,
               org.springframework.core.env.Environment environment) {
        this.authProperties = authProperties;
        this.environment = environment;
    }

    @PostConstruct
    void init() {
        String secret = authProperties.getSecret();
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            log.warn("JWT secret is too short or missing. Using default (development only).");
            secret = DEFAULT_SECRET;
        }
        if (isProductionProfile() && DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT secret must be set in production. Do not use the default secret.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isProductionProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate access token for user.
     * Contains user ID, email, and roles.
     */
    public String generateAccessToken(UserDto user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(authProperties.getAccess().getExpiration());
        List<String> roleNames = user.roles().stream().map(Role::name).collect(Collectors.toList());

        return Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("roles", roleNames)
                .claim("name", user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generate a new refresh token (random string).
     * Returns the plain token - caller must hash before storing.
     */
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hash a refresh token for storage.
     * Uses SHA-256.
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Calculate refresh token expiration time.
     */
    public LocalDateTime getRefreshTokenExpiry() {
        return LocalDateTime.now().plusSeconds(authProperties.getRefresh().getExpiration() / 1000);
    }

    /**
     * Validate access token and extract claims.
     * Returns empty if token is invalid or expired.
     * Supports both legacy "role" (single) and "roles" (list) claims for backward compatibility.
     */
    public Optional<TokenClaims> validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            List<Role> roles;
            @SuppressWarnings("unchecked")
            List<String> roleNames = claims.get("roles", List.class);
            if (roleNames != null && !roleNames.isEmpty()) {
                roles = roleNames.stream().map(Role::valueOf).toList();
            } else {
                String singleRole = claims.get("role", String.class);
                roles = singleRole != null ? List.of(Role.valueOf(singleRole)) : List.of();
            }

            return Optional.of(new TokenClaims(
                    UUID.fromString(claims.getSubject()),
                    claims.get("email", String.class),
                    roles,
                    claims.get("name", String.class)
            ));
        } catch (ExpiredJwtException e) {
            log.debug("Access token expired: {}", e.getMessage());
            return Optional.empty();
        } catch (JwtException e) {
            log.debug("Invalid access token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get access token cookie max age in seconds.
     */
    public int getAccessTokenMaxAge() {
        return (int) (authProperties.getAccess().getExpiration() / 1000);
    }

    /**
     * Get refresh token cookie max age in seconds.
     */
    public int getRefreshTokenMaxAge() {
        return (int) (authProperties.getRefresh().getExpiration() / 1000);
    }

    /**
     * Parsed token claims.
     */
    public record TokenClaims(
            UUID userId,
            String email,
            List<Role> roles,
            String name
    ) {}
}
