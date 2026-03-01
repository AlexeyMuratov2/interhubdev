package com.example.interhubdev.otp;

import com.example.interhubdev.error.AppException;

/**
 * Public API for OTP: create, verify and consume.
 * <p>
 * Purpose identifies the scenario (e.g. "password-change", "email-verify"). Subject identifies the entity
 * (e.g. user ID, email). Both are used in Redis keys (subject is hashed for storage).
 * <p>
 * Errors are thrown as {@link AppException} (handled by global handler).
 */
public interface OtpApi {

    /**
     * Create a new OTP for the given purpose and subject.
     * Stores only a hash of the code in Redis; returns the plain code for the caller to deliver (e.g. send by email).
     * Any previous OTP for the same purpose+subject is replaced. Rate limit applies: too frequent requests throw.
     *
     * @param purpose scenario identifier (e.g. "password-change", "email-verify"); must be non-blank
     * @param subject entity identifier (e.g. user UUID, email); must be non-blank
     * @param options optional overrides; use {@link OtpOptions#defaults()} for defaults
     * @return created OTP with plain code and expiry; caller must deliver plainCode to user once
     * @throws AppException if rate limit exceeded, or invalid purpose/subject
     */
    OtpCreatedResult create(String purpose, String subject, OtpOptions options);

    /**
     * Verify the given code and consume the OTP on success (one-time use).
     * Uses constant-time comparison. Increments attempt counter on failure; too many attempts throw.
     *
     * @param purpose same purpose used in create
     * @param subject same subject used in create
     * @param code    user-supplied code (plain)
     * @return true if code matched and OTP was consumed, false if code invalid or expired
     * @throws AppException if max verify attempts exceeded for this purpose+subject
     */
    boolean verifyAndConsume(String purpose, String subject, String code);

    /**
     * Check whether Redis is available (for health checks or conditional features).
     *
     * @return true if the OTP store is operational
     */
    boolean isOperational();
}
