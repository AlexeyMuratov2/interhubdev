/**
 * Authentication module - handles login, JWT tokens, and security configuration.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"user", "error"}
)
package com.example.interhubdev.auth;
