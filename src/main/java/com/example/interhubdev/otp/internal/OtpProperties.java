package com.example.interhubdev.otp.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for OTP module. Values can be overridden per-call via {@link com.example.interhubdev.otp.OtpOptions}.
 */
@Component
@ConfigurationProperties(prefix = "app.otp")
@Getter
@Setter
class OtpProperties {

    /** Default TTL for OTP in minutes. */
    private int defaultTtlMinutes = 15;

    /** Default OTP code length (digits). */
    private int defaultCodeLength = 6;

    /** Minimum seconds between two create requests for the same purpose+subject. */
    private int minRateLimitSeconds = 60;

    /** Max failed verify attempts per purpose+subject before blocking. */
    private int maxVerifyAttempts = 5;

    /** Window in minutes for counting verify attempts (attempts key TTL). */
    private int attemptsWindowMinutes = 15;
}
