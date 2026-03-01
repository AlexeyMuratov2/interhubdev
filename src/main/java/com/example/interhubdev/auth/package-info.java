/**
 * Authentication module - handles login, JWT tokens, and security configuration.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"user", "error", "email", "otp"}
)
package com.example.interhubdev.auth;
