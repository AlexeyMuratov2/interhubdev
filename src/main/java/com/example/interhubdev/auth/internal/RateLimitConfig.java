package com.example.interhubdev.auth.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares rate limit filter bean to avoid circular dependency with SecurityConfig.
 */
@Configuration
class RateLimitConfig {

    @Bean
    RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter();
    }
}
