package com.example.interhubdev.outbox;

import java.time.Instant;
import java.util.Optional;

/**
 * Builder for outbox event with optional metadata.
 * <p>
 * Usage:
 * <pre>{@code
 * publisher.publish(OutboxEventDraft.builder()
 *     .eventType("attendance.absence_notice_submitted")
 *     .payload(Map.of("id", id))
 *     .occurredAt(Instant.now())
 *     .correlationId("req-123")
 *     .build());
 * }</pre>
 */
public final class OutboxEventDraft {

    private final String eventType;
    private final Object payload;
    private final Instant occurredAt;
    private final String correlationId;
    private final String traceId;

    private OutboxEventDraft(Builder builder) {
        this.eventType = builder.eventType;
        this.payload = builder.payload;
        this.occurredAt = builder.occurredAt != null ? builder.occurredAt : Instant.now();
        this.correlationId = builder.correlationId;
        this.traceId = builder.traceId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String eventType() {
        return eventType;
    }

    public Object payload() {
        return payload;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public Optional<String> correlationId() {
        return Optional.ofNullable(correlationId);
    }

    public Optional<String> traceId() {
        return Optional.ofNullable(traceId);
    }

    public static class Builder {
        private String eventType;
        private Object payload;
        private Instant occurredAt;
        private String correlationId;
        private String traceId;

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public OutboxEventDraft build() {
            if (eventType == null || eventType.isBlank()) {
                throw new IllegalArgumentException("eventType is required");
            }
            if (payload == null) {
                throw new IllegalArgumentException("payload is required");
            }
            return new OutboxEventDraft(this);
        }
    }
}
