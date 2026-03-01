package com.example.interhubdev.otp;

import java.time.Instant;

/**
 * Result of creating an OTP.
 * <p>
 * The plain code must be delivered to the user (e.g. by email) and must not be logged or stored elsewhere.
 */
public record OtpCreatedResult(
    /**
     * The generated OTP in plain form. Single-use; deliver to user once.
     */
    String plainCode,
    /**
     * When this OTP expires. After this time, verification will fail.
     */
    Instant expiresAt
) {}
