package com.example.interhubdev.otp;

import java.util.Optional;

/**
 * Optional overrides for a single OTP create request.
 * When a field is empty, module defaults (from configuration) are used.
 */
public record OtpOptions(
    /**
     * OTP validity in minutes. If empty, uses app.otp.default-ttl-minutes.
     */
    Optional<Integer> ttlMinutes,
    /**
     * Code length (digits). If empty, uses app.otp.default-code-length.
     */
    Optional<Integer> codeLength,
    /**
     * Minimum seconds between two create requests for the same purpose+subject. If empty, uses app.otp.min-rate-limit-seconds.
     */
    Optional<Integer> rateLimitSeconds
) {
    /**
     * Default options: use all module defaults.
     */
    public static OtpOptions defaults() {
        return new OtpOptions(Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Builder-style override for TTL only.
     */
    public static OtpOptions ttlMinutes(int minutes) {
        return new OtpOptions(Optional.of(minutes), Optional.empty(), Optional.empty());
    }
}
