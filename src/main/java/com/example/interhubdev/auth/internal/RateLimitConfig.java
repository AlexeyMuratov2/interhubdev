package com.example.interhubdev.auth.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares rate limit filter bean to avoid circular dependency with SecurityConfig.
 */
@Configuration
@RequiredArgsConstructor
class RateLimitConfig {

    private final AuthProperties authProperties;

    @Bean
    RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter(authProperties);
    }
}
