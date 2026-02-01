package com.example.interhubdev.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for task scheduling.
 * Provides TaskScheduler bean for invitation email retry mechanism.
 */
@Configuration
class SchedulingConfig {

    @Bean
    TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setErrorHandler(t -> {
            // Log errors but don't crash
            System.err.println("Scheduled task error: " + t.getMessage());
        });
        return scheduler;
    }
}
