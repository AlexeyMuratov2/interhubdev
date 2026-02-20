package com.example.interhubdev.outbox.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
     * Registers JavaTimeModule so payloads with java.time types (e.g. Instant) serialize correctly.
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
