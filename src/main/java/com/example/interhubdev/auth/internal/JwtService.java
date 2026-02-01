package com.example.interhubdev.auth.internal;

import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for JWT token generation and validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class JwtService {

    private final AuthProperties authProperties;
    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    void init() {
        String secret = authProperties.getSecret();
        if (secret == null || secret.length() < 32) {
            log.warn("JWT secret is too short! Using default, but this is NOT secure for production.");
            secret = "default-secret-key-for-development-only-change-in-production-min-32-chars";
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate access token for user.
     * Contains user ID, email, and role.
     */
    public String generateAccessToken(UserDto user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(authProperties.getAccess().getExpiration());

        return Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("role", user.role().name())
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
     */
    public Optional<TokenClaims> validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new TokenClaims(
                    UUID.fromString(claims.getSubject()),
                    claims.get("email", String.class),
                    Role.valueOf(claims.get("role", String.class)),
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
     * Extract user ID from token without full validation.
     * Useful for logging/debugging. Returns empty if parsing fails.
     */
    public Optional<UUID> extractUserIdUnsafe(String token) {
        try {
            // Parse without validation to get subject
            String[] parts = token.split("\\.");
            if (parts.length != 3) return Optional.empty();
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            // Simple extraction - in production use proper JSON parsing
            int subStart = payload.indexOf("\"sub\":\"") + 7;
            int subEnd = payload.indexOf("\"", subStart);
            return Optional.of(UUID.fromString(payload.substring(subStart, subEnd)));
        } catch (Exception e) {
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
            Role role,
            String name
    ) {}
}
