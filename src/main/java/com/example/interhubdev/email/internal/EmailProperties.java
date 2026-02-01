package com.example.interhubdev.email.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the email service.
 */
@ConfigurationProperties(prefix = "app.email")
@Validated
@Getter
@Setter
class EmailProperties {

    /**
     * Default sender email address.
     */
    @NotBlank(message = "Default sender email is required")
    @Email(message = "Default sender must be a valid email")
    private String from;

    /**
     * Default sender display name (optional).
     */
    private String fromName = "InterHubDev";

    /**
     * Whether email sending is enabled.
     * Set to false for development/testing to disable actual sending.
     */
    private boolean enabled = true;

    /**
     * Whether to log emails instead of sending (for development).
     */
    private boolean logOnly = false;

    /**
     * Base URL for links in emails (e.g., https://app.interhubdev.com).
     */
    private String baseUrl = "http://localhost:3000";
}
