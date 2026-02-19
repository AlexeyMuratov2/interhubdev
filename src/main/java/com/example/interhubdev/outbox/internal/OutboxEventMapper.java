package com.example.interhubdev.outbox.internal;

import com.example.interhubdev.outbox.OutboxEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps OutboxEventEntity to OutboxEvent DTO for handlers.
 * Package-private: only accessible within the outbox module.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class OutboxEventMapper {

    private final ObjectMapper objectMapper;

    /**
     * Convert entity to DTO for handler processing.
     *
     * @param entity outbox event entity
     * @return outbox event DTO
     */
    OutboxEvent toDto(OutboxEventEntity entity) {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    entity.getPayloadJson(),
                    new TypeReference<Map<String, Object>>() {}
            );

            return new OutboxEvent(
                    entity.getId(),
                    entity.getEventType(),
                    payload,
                    entity.getOccurredAt(),
                    entity.getCreatedAt(),
                    entity.getAttempts(),
                    entity.getCorrelationId(),
                    entity.getTraceId()
            );
        } catch (Exception e) {
            log.error("Failed to deserialize event payload: id={}, type={}", 
                    entity.getId(), entity.getEventType(), e);
            throw new RuntimeException("Failed to deserialize event payload", e);
        }
    }
}
