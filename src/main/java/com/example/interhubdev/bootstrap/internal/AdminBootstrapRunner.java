package com.example.interhubdev.bootstrap.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Application startup listener that triggers the bootstrap process.
 * 
 * <p>This component listens for {@link ApplicationReadyEvent} and delegates
 * the actual bootstrap logic to {@link BootstrapServiceImpl}.</p>
 * 
 * <p>Unlike regular users who go through the invite flow (PENDING → ACTIVE),
 * the bootstrap SUPER_ADMIN is created directly with ACTIVE status and password set.
 * This is necessary because there must be at least one admin to invite other users.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
class AdminBootstrapRunner {

    private final BootstrapServiceImpl bootstrapService;

    /**
     * Triggers bootstrap when application is fully ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready, starting bootstrap process...");
        
        boolean success = bootstrapService.executeBootstrap();
        
        if (success) {
            log.info("Bootstrap process completed successfully");
        } else {
            log.error("Bootstrap process failed! Check configuration and logs.");
        }
    }
}
