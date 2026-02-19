package com.example.interhubdev.outbox.internal;

import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of OutboxIntegrationEventPublisher.
 * Writes events to outbox_event table in the same transaction.
 * Package-private: only accessible within the outbox module.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class OutboxIntegrationEventPublisherImpl implements OutboxIntegrationEventPublisher {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publish(String eventType, Object payload) {
        publish(OutboxEventDraft.builder()
                .eventType(eventType)
                .payload(payload)
                .build());
    }

    @Override
    @Transactional
    public void publish(OutboxEventDraft draft) {
        try {
            String payloadJson = objectMapper.writeValueAsString(draft.payload());

            OutboxEventEntity entity = OutboxEventEntity.builder()
                    .eventType(draft.eventType())
                    .payloadJson(payloadJson)
                    .occurredAt(draft.occurredAt())
                    .createdAt(Instant.now())
                    .status(OutboxEventStatus.NEW)
                    .attempts(0)
                    .correlationId(draft.correlationId().orElse(null))
                    .traceId(draft.traceId().orElse(null))
                    .build();

            repository.save(entity);

            log.debug("Published outbox event: type={}, id={}", draft.eventType(), entity.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload: type={}", draft.eventType(), e);
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }
}
