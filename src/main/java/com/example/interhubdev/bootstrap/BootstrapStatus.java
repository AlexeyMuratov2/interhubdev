package com.example.interhubdev.bootstrap;

/**
 * Status of the application bootstrap process.
 */
public enum BootstrapStatus {
    /**
     * Bootstrap has not been attempted yet.
     * Application is still starting up.
     */
    NOT_STARTED,

    /**
     * Bootstrap completed successfully.
     * SUPER_ADMIN user exists (either created or already existed).
     */
    COMPLETED,

    /**
     * Bootstrap failed due to configuration error.
     * Check application logs for details.
     */
    FAILED
}
