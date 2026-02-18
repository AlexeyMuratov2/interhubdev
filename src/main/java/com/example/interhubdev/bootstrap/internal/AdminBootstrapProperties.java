package com.example.interhubdev.bootstrap.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the initial SUPER_ADMIN user.
 * These values are read from environment variables via .env file.
 */
@ConfigurationProperties(prefix = "app.admin")
@Validated
@Getter
@Setter
class AdminBootstrapProperties {

    /**
     * Email address for the initial SUPER_ADMIN user.
     */
    @NotBlank(message = "SUPER_ADMIN email is required")
    @Email(message = "SUPER_ADMIN email must be valid")
    private String email;

    /**
     * Password for the initial SUPER_ADMIN user.
     * Will be BCrypt encoded before storing.
     * If empty, bootstrap will be skipped (useful for development when bootstrap is disabled).
     */
    @Size(min = 8, message = "SUPER_ADMIN password must be at least 8 characters if provided")
    private String password;

    /**
     * First name for the initial SUPER_ADMIN user (optional).
     */
    private String firstName;

    /**
     * Last name for the initial SUPER_ADMIN user (optional).
     */
    private String lastName;
}
