package com.example.interhubdev.invitation.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for the invitation module.
 */
@ConfigurationProperties(prefix = "app.invitation")
@Validated
@Getter
@Setter
class InvitationProperties {

    /**
     * How long an invitation remains valid.
     * Default: 90 days (3 months).
     */
    private Duration expiryDuration = Duration.ofDays(90);

    /**
     * How long a token remains valid.
     * Default: 24 hours.
     */
    private Duration tokenExpiryDuration = Duration.ofHours(24);

    /**
     * Maximum number of email send attempts.
     * Default: 3.
     */
    private int maxEmailAttempts = 3;

    /**
     * Delay between email retry attempts (in seconds).
     * Actual delay = retryDelay * attemptNumber.
     * Default: 60 seconds.
     */
    private int retryDelaySeconds = 60;

    /**
     * Base URL for invitation links.
     * Used to construct the activation link in emails.
     */
    private String baseUrl = "http://localhost:5173";
}
