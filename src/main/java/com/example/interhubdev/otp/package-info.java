/**
 * OTP (One-Time Password) module — generation, storage in Redis, and verification.
 * <p>
 * Reusable for different scenarios: password change, email verification, 2FA, etc.
 * <p>
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.otp.OtpApi} — create OTP, verify and consume</li>
 *   <li>{@link com.example.interhubdev.otp.OtpOptions} — optional overrides (TTL, code length, rate limit)</li>
 *   <li>{@link com.example.interhubdev.otp.OtpCreatedResult} — result of create (plain code for sending, expiry)</li>
 * </ul>
 * <h2>Security</h2>
 * <ul>
 *   <li>OTP value stored as hash (SHA-256) in Redis; subject hashed in key to avoid leaking in storage</li>
 *   <li>Constant-time comparison on verify; single-use (consumed on success); rate limits on create and verify</li>
 * </ul>
 * <h2>Dependencies</h2>
 * Depends only on {@code error} for throwing {@link com.example.interhubdev.error.AppException}.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "OTP",
    allowedDependencies = {"error"}
)
package com.example.interhubdev.otp;
