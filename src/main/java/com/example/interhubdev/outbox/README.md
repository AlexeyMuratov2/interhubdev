# Outbox Module

Infrastructure module for reliable integration event delivery using the transactional outbox pattern.

## Overview

The Outbox module provides a transactional outbox implementation that ensures integration events are reliably delivered even if the publishing transaction commits successfully. Events are written to the `outbox_event` table in the same database transaction, and a background processor picks them up and delivers them through registered handlers.

## Architecture

### Components

1. **OutboxIntegrationEventPublisher** - Public API for publishing events
2. **OutboxEventRepository** - JPA repository with atomic locking support (PostgreSQL FOR UPDATE SKIP LOCKED)
3. **OutboxProcessor** - Scheduled job that processes events
4. **OutboxHandlerRegistry** - Registry for event handlers
5. **OutboxEventHandler** - Interface for event handlers

### Flow

1. Domain module calls `publisher.publish(eventType, payload)` within a transaction
2. Event is written to `outbox_event` table with status `NEW`
3. `OutboxProcessor` (scheduled job) periodically:
   - Releases stale locks (from dead workers)
   - Locks a batch of `NEW`/`FAILED` events using `FOR UPDATE SKIP LOCKED`
   - Updates status to `PROCESSING` with lock metadata
   - Finds handler by `event_type`
   - Calls `handler.handle(event)`
   - On success: marks as `DONE`
   - On failure: marks as `FAILED` with exponential backoff retry

## Usage

### Publishing Events

```java
@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final OutboxIntegrationEventPublisher publisher;
    
    @Transactional
    public void submitAbsenceNotice(UUID absenceNoticeId, UUID studentId) {
        // ... business logic ...
        
        // Publish event in the same transaction
        publisher.publish("attendance.absence_notice_submitted", Map.of(
            "absenceNoticeId", absenceNoticeId,
            "studentId", studentId
        ));
    }
}
```

### Creating Event Handlers

```java
@Component
@RequiredArgsConstructor
public class AbsenceNoticeHandler implements OutboxEventHandler {
    private final NotificationService notificationService;
    
    @Override
    public String eventType() {
        return "attendance.absence_notice_submitted";
    }
    
    @Override
    public void handle(OutboxEvent event) throws Exception {
        Map<String, Object> payload = event.getPayload();
        UUID absenceNoticeId = UUID.fromString(
            payload.get("absenceNoticeId").toString()
        );
        UUID studentId = UUID.fromString(
            payload.get("studentId").toString()
        );
        
        notificationService.sendAbsenceNoticeNotification(
            absenceNoticeId, studentId
        );
    }
}
```

### Event Metadata

For advanced use cases, use `OutboxEventDraft`:

```java
publisher.publish(OutboxEventDraft.builder()
    .eventType("attendance.absence_notice_submitted")
    .payload(Map.of("id", id))
    .occurredAt(Instant.now())
    .correlationId("req-123")
    .traceId("trace-456")
    .build());
```

## Configuration

Add to `application.properties`:

```properties
# Outbox Module Configuration
outbox.processor.enabled=true
outbox.processor.interval=10000                    # Processing interval in milliseconds
outbox.processor.batch-size=50                     # Events per batch
outbox.processor.max-attempts=10                  # Max retry attempts
outbox.processor.base-retry-delay-seconds=30     # Base retry delay
outbox.processor.max-retry-delay-seconds=1800     # Max retry delay (30 minutes)
outbox.processor.lock-stale-timeout-seconds=300    # Stale lock timeout (5 minutes)
outbox.processor.worker-id=default                 # Worker instance identifier
```

## Event Statuses

- **NEW** - Event is new and ready for processing
- **PROCESSING** - Event is currently being processed by a worker
- **DONE** - Event was successfully processed
- **FAILED** - Event processing failed and will be retried (or permanently failed if max attempts reached)

## Retry Logic

Events are retried with exponential backoff:

- Delay = `min(maxDelay, baseDelay * 2^(attempts-1)) + jitter(0..baseDelay)`
- After `maxAttempts`, events are marked as permanently failed (`nextRetryAt = null`)

## Concurrency

The module supports multiple worker instances:

- Uses PostgreSQL `FOR UPDATE SKIP LOCKED` for safe concurrent processing
- Each worker locks a batch atomically
- Stale locks are automatically released after `lock-stale-timeout-seconds`

## Database Schema

See `V40__create_outbox_event.sql` for the complete schema.

Key fields:
- `id` - UUID primary key
- `event_type` - Event type identifier (e.g., "attendance.absence_notice_submitted")
- `payload_json` - JSONB payload
- `status` - Event status (NEW, PROCESSING, DONE, FAILED)
- `attempts` - Number of processing attempts
- `next_retry_at` - When to retry (null for permanently failed)
- `locked_by` - Worker instance identifier
- `locked_at` - Lock timestamp

## Monitoring

Query events by status:

```sql
SELECT status, COUNT(*) 
FROM outbox_event 
GROUP BY status;
```

Find failed events:

```sql
SELECT * 
FROM outbox_event 
WHERE status = 'FAILED' 
ORDER BY occurred_at DESC;
```

## TODO: Future Enhancements

1. **Connect domain modules** - Add `publisher.publish()` calls in Attendance, Grades, etc. modules
2. **Notification module** - Create notification handlers for events
3. **Idempotency** - Add idempotency/deduplication for downstream consumers
4. **Metrics** - Add Micrometer metrics for monitoring
5. **Admin endpoint** - Optional read-only REST endpoint for viewing events
6. **Dead letter queue** - Separate handling for permanently failed events

## Testing

See `OutboxEventRepositoryTest` and `OutboxProcessorTest` for integration test examples.

## Dependencies

- `error` module - For error handling (if needed)

This is an infrastructure module. Domain modules depend on it, but it does not depend on domain modules.
