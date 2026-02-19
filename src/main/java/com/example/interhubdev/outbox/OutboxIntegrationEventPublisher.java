package com.example.interhubdev.outbox;

import java.time.Instant;

/**
 * Publisher for integration events using transactional outbox pattern.
 * <p>
 * Events published through this interface are written to the outbox_event table
 * in the same database transaction. If the transaction commits, the event is
 * guaranteed to be processed by the OutboxProcessor.
 * <p>
 * Usage:
 * <pre>{@code
 * @Transactional
 * public void createAbsenceNotice(UUID id) {
 *     // ... business logic ...
 *     publisher.publish("attendance.absence_notice_submitted", Map.of("id", id));
 * }
 * }</pre>
 */
public interface OutboxIntegrationEventPublisher {

    /**
     * Publish an integration event.
     * <p>
     * The event will be written to outbox_event table with status NEW.
     * The payload will be serialized to JSON.
     * <p>
     * This method should be called within a transaction. If the transaction
     * rolls back, the event will not be persisted.
     *
     * @param eventType event type identifier (e.g., "attendance.absence_notice_submitted")
     * @param payload event payload (will be serialized to JSON using Jackson)
     */
    void publish(String eventType, Object payload);

    /**
     * Publish an integration event with metadata control.
     * <p>
     * Allows specifying occurredAt timestamp and optional correlationId/traceId.
     *
     * @param draft event draft with type, payload, and optional metadata
     */
    void publish(OutboxEventDraft draft);
}
