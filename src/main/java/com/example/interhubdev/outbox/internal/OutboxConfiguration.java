package com.example.interhubdev.outbox.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Outbox module.
 * Provides ObjectMapper bean if not already available in the application context.
 * Package-private: only accessible within the outbox module.
 */
@Configuration
class OutboxConfiguration {

    /**
     * Create ObjectMapper bean if not already present.
     * Spring Boot usually provides this automatically when spring-boot-starter-web is present,
     * but this ensures it's available for the outbox module.
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
