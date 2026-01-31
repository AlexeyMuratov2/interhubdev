/**
 * Bootstrap module - handles initial system setup.
 * Creates the initial SUPER_ADMIN user from environment variables on application startup.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"user"}
)
package com.example.interhubdev.bootstrap;
