package com.example.interhubdev.bootstrap;

/**
 * Public API for the Bootstrap module.
 * Provides information about the initial system setup status.
 */
public interface BootstrapApi {

    /**
     * Check if bootstrap process has completed successfully.
     *
     * @return true if SUPER_ADMIN exists and system is ready
     */
    boolean isCompleted();

    /**
     * Get the current bootstrap status.
     *
     * @return current status of the bootstrap process
     */
    BootstrapStatus getStatus();

    /**
     * Get the configured SUPER_ADMIN email address.
     * Useful for health checks and diagnostics.
     *
     * @return configured admin email from environment
     */
    String getConfiguredAdminEmail();

    /**
     * Check if the SUPER_ADMIN was created during this application startup.
     * Returns false if admin already existed before startup.
     *
     * @return true if admin was created during current startup
     */
    boolean wasAdminCreatedOnStartup();
}
