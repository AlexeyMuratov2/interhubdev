package com.example.interhubdev.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an outbox event for handler processing.
 * <p>
 * This is a read-only view of the event passed to handlers.
 * Handlers should not modify the event.
 */
public final class OutboxEvent {

    private final UUID id;
    private final String eventType;
    private final Map<String, Object> payload;
    private final Instant occurredAt;
    private final Instant createdAt;
    private final int attempts;
    private final String correlationId;
    private final String traceId;

    public OutboxEvent(
            UUID id,
            String eventType,
            Map<String, Object> payload,
            Instant occurredAt,
            Instant createdAt,
            int attempts,
            String correlationId,
            String traceId) {
        this.id = id;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
        this.attempts = attempts;
        this.correlationId = correlationId;
        this.traceId = traceId;
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getTraceId() {
        return traceId;
    }
}
