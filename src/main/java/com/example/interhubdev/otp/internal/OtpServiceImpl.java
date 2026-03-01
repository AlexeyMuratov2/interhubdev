package com.example.interhubdev.otp.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.otp.OtpApi;
import com.example.interhubdev.otp.OtpCreatedResult;
import com.example.interhubdev.otp.OtpOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

/**
 * OTP service: generates codes, stores hash in Redis, verifies with constant-time comparison and consumes on success.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class OtpServiceImpl implements OtpApi {

    private static final String KEY_PREFIX_DATA = "otp:data:";
    private static final String KEY_PREFIX_RATE = "otp:rate:";
    private static final String KEY_PREFIX_ATTEMPTS = "otp:attempts:";
    private static final int SUBJECT_HASH_HEX_LEN = 32;
    private static final String DIGEST_ALGORITHM = "SHA-256";

    private final StringRedisTemplate redisTemplate;
    private final OtpProperties properties;

    @Override
    public OtpCreatedResult create(String purpose, String subject, OtpOptions options) {
        validatePurposeAndSubject(purpose, subject);
        String subjectKey = toStorageKey(subject);
        int ttlMinutes = options.ttlMinutes().orElse(properties.getDefaultTtlMinutes());
        int codeLength = options.codeLength().orElse(properties.getDefaultCodeLength());
        int rateLimitSeconds = options.rateLimitSeconds().orElse(properties.getMinRateLimitSeconds());

        try {
            enforceRateLimit(purpose, subjectKey, rateLimitSeconds);

            String plainCode = generateCode(codeLength);
            String codeHash = hashToHex(plainCode);
            String dataKey = KEY_PREFIX_DATA + purpose + ":" + subjectKey;
            long ttlSeconds = ttlMinutes * 60L;

            redisTemplate.opsForValue().set(dataKey, codeHash, java.time.Duration.ofSeconds(ttlSeconds));

            String rateKey = KEY_PREFIX_RATE + purpose + ":" + subjectKey;
            redisTemplate.opsForValue().set(rateKey, "1", java.time.Duration.ofSeconds(rateLimitSeconds));

            Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
            log.debug("OTP created for purpose={}, subjectKey={}, expiresAt={}", purpose, subjectKey, expiresAt);
            return new OtpCreatedResult(plainCode, expiresAt);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable during OTP create: {}", e.getMessage());
            throw OtpErrors.serviceUnavailable("OTP service temporarily unavailable. Please try again later.");
        }
    }

    @Override
    public boolean verifyAndConsume(String purpose, String subject, String code) {
        if (purpose == null || purpose.isBlank() || subject == null || subject.isBlank()) {
            throw OtpErrors.invalidArgument("Purpose and subject are required");
        }
        if (code == null || code.isBlank()) {
            return false;
        }

        String subjectKey = toStorageKey(subject);
        String dataKey = KEY_PREFIX_DATA + purpose + ":" + subjectKey;
        String attemptsKey = KEY_PREFIX_ATTEMPTS + purpose + ":" + subjectKey;

        try {
            enforceMaxAttempts(attemptsKey);

            String storedHash = redisTemplate.opsForValue().get(dataKey);
            if (storedHash == null) {
                incrementAttempts(attemptsKey);
                return false;
            }

            String inputHash = hashToHex(code);
            if (!constantTimeEquals(inputHash, storedHash)) {
                incrementAttempts(attemptsKey);
                return false;
            }

            redisTemplate.delete(dataKey);
            redisTemplate.delete(attemptsKey);
            log.debug("OTP consumed for purpose={}, subjectKey={}", purpose, subjectKey);
            return true;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable during OTP verify: {}", e.getMessage());
            throw OtpErrors.serviceUnavailable("OTP service temporarily unavailable. Please try again later.");
        }
    }

    @Override
    public boolean isOperational() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.debug("Redis ping failed: {}", e.getMessage());
            return false;
        }
    }

    private void validatePurposeAndSubject(String purpose, String subject) {
        if (purpose == null || purpose.isBlank()) {
            throw OtpErrors.invalidArgument("Purpose is required");
        }
        if (subject == null || subject.isBlank()) {
            throw OtpErrors.invalidArgument("Subject is required");
        }
        if (purpose.contains(":") || purpose.contains("\u0000")) {
            throw OtpErrors.invalidArgument("Purpose must not contain ':' or null character");
        }
    }

    private void enforceRateLimit(String purpose, String subjectKey, int rateLimitSeconds) {
        String rateKey = KEY_PREFIX_RATE + purpose + ":" + subjectKey;
        Boolean exists = redisTemplate.hasKey(rateKey);
        if (Boolean.TRUE.equals(exists)) {
            throw OtpErrors.rateLimit("Please wait before requesting a new code. Try again in a few minutes.");
        }
    }

    private void enforceMaxAttempts(String attemptsKey) {
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int current = attemptsStr != null ? parseIntSafe(attemptsStr, 0) : 0;
        if (current >= properties.getMaxVerifyAttempts()) {
            throw OtpErrors.tooManyAttempts("Too many failed attempts. Please request a new code.");
        }
    }

    private void incrementAttempts(String attemptsKey) {
        long ttlSeconds = properties.getAttemptsWindowMinutes() * 60L;
        Long inc = redisTemplate.opsForValue().increment(attemptsKey);
        if (inc == null) {
            inc = 1L;
        }
        if (inc == 1) {
            redisTemplate.expire(attemptsKey, java.time.Duration.ofSeconds(ttlSeconds));
        }
    }

    private static String generateCode(int length) {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(rng.nextInt(10));
        }
        return sb.toString();
    }

    private static String hashToHex(String input) {
        MessageDigest md = getSha256();
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }

    /**
     * Storage key from subject: hash so Redis keys don't contain raw emails/IDs.
     */
    private static String toStorageKey(String subject) {
        MessageDigest md = getSha256();
        byte[] digest = md.digest(subject.getBytes(StandardCharsets.UTF_8));
        String hex = bytesToHex(digest);
        return hex.length() >= SUBJECT_HASH_HEX_LEN ? hex.substring(0, SUBJECT_HASH_HEX_LEN) : hex;
    }

    private static MessageDigest getSha256() {
        try {
            return MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(DIGEST_ALGORITHM + " not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length() != b.length()) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(aBytes, bBytes);
    }

    private static int parseIntSafe(String s, int defaultValue) {
        try {
            return Integer.parseInt(s, 10);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
