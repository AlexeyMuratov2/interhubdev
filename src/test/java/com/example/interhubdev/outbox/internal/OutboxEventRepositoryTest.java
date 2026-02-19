package com.example.interhubdev.outbox.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OutboxEventRepository.
 * Tests atomic locking, status transitions, and retry logic.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("OutboxEventRepository")
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository repository;

    @Nested
    @DisplayName("lockNextBatch")
    class LockNextBatch {

        @Test
        @DisplayName("returns empty list when no events ready")
        void noEvents() {
            Instant now = Instant.now();
            List<OutboxEventEntity> events = repository.lockNextBatch(10, now);
            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("locks NEW events ready for processing")
        void locksNewEvents() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType("test.event")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.NEW)
                    .attempts(0)
                    .build();
            repository.save(event);

            Instant now = Instant.now();
            List<OutboxEventEntity> events = repository.lockNextBatch(10, now);

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getId()).isEqualTo(event.getId());
        }

        @Test
        @DisplayName("does not lock events with future nextRetryAt")
        void skipsFutureRetries() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType("test.event")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.FAILED)
                    .attempts(1)
                    .nextRetryAt(Instant.now().plusSeconds(60))
                    .build();
            repository.save(event);

            Instant now = Instant.now();
            List<OutboxEventEntity> events = repository.lockNextBatch(10, now);

            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("locks FAILED events with past nextRetryAt")
        void locksFailedEventsReadyForRetry() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType("test.event")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.FAILED)
                    .attempts(1)
                    .nextRetryAt(Instant.now().minusSeconds(10))
                    .build();
            repository.save(event);

            Instant now = Instant.now();
            List<OutboxEventEntity> events = repository.lockNextBatch(10, now);

            assertThat(events).hasSize(1);
        }

        @Test
        @DisplayName("respects batch size limit")
        void respectsBatchSize() {
            for (int i = 0; i < 15; i++) {
                OutboxEventEntity event = OutboxEventEntity.builder()
                        .eventType("test.event" + i)
                        .payloadJson("{}")
                        .occurredAt(Instant.now())
                        .status(OutboxEventStatus.NEW)
                        .attempts(0)
                        .build();
                repository.save(event);
            }

            Instant now = Instant.now();
            List<OutboxEventEntity> events = repository.lockNextBatch(10, now);

            assertThat(events).hasSize(10);
        }
    }

    @Nested
    @DisplayName("markDone")
    class MarkDone {

        @Test
        @DisplayName("marks event as DONE and clears lock fields")
        void marksDone() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType("test.event")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.PROCESSING)
                    .lockedBy("worker-1")
                    .lockedAt(Instant.now())
                    .build();
            repository.save(event);

            Instant processedAt = Instant.now();
            repository.markDone(event.getId(), processedAt);

            OutboxEventEntity updated = repository.findById(event.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.DONE);
            assertThat(updated.getProcessedAt()).isEqualTo(processedAt);
            assertThat(updated.getLockedBy()).isNull();
            assertThat(updated.getLockedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("markFailed")
    class MarkFailed {

        @Test
        @DisplayName("marks event as FAILED with error and next retry")
        void marksFailed() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType("test.event")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.PROCESSING)
                    .attempts(1)
                    .build();
            repository.save(event);

            Instant nextRetry = Instant.now().plusSeconds(60);
            repository.markFailed(event.getId(), "Test error", 2, nextRetry);

            OutboxEventEntity updated = repository.findById(event.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(updated.getLastError()).isEqualTo("Test error");
            assertThat(updated.getAttempts()).isEqualTo(2);
            assertThat(updated.getNextRetryAt()).isEqualTo(nextRetry);
            assertThat(updated.getLockedBy()).isNull();
            assertThat(updated.getLockedAt()).isNull();
        }

        @Test
        @DisplayName("can mark as permanently failed with null nextRetryAt")
        void permanentFailure() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType("test.event")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.PROCESSING)
                    .attempts(9)
                    .build();
            repository.save(event);

            repository.markFailed(event.getId(), "Max attempts", 10, null);

            OutboxEventEntity updated = repository.findById(event.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(updated.getAttempts()).isEqualTo(10);
            assertThat(updated.getNextRetryAt()).isNull();
        }
    }

    @Nested
    @DisplayName("releaseStaleLocks")
    class ReleaseStaleLocks {

        @Test
        @DisplayName("releases stale PROCESSING locks")
        void releasesStaleLocks() {
            OutboxEventEntity stale = OutboxEventEntity.builder()
                    .eventType("test.event1")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.PROCESSING)
                    .lockedBy("dead-worker")
                    .lockedAt(Instant.now().minusSeconds(600))
                    .build();
            repository.save(stale);

            OutboxEventEntity fresh = OutboxEventEntity.builder()
                    .eventType("test.event2")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.PROCESSING)
                    .lockedBy("alive-worker")
                    .lockedAt(Instant.now().minusSeconds(60))
                    .build();
            repository.save(fresh);

            Instant staleBefore = Instant.now().minusSeconds(300);
            int released = repository.releaseStaleLocks(staleBefore);

            assertThat(released).isEqualTo(1);

            OutboxEventEntity updatedStale = repository.findById(stale.getId()).orElseThrow();
            assertThat(updatedStale.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(updatedStale.getLockedBy()).isNull();
            assertThat(updatedStale.getLockedAt()).isNull();

            OutboxEventEntity updatedFresh = repository.findById(fresh.getId()).orElseThrow();
            assertThat(updatedFresh.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
            assertThat(updatedFresh.getLockedBy()).isEqualTo("alive-worker");
        }
    }

    @Nested
    @DisplayName("updateToProcessing")
    class UpdateToProcessing {

        @Test
        @DisplayName("updates events to PROCESSING with lock fields")
        void updatesToProcessing() {
            OutboxEventEntity event1 = OutboxEventEntity.builder()
                    .eventType("test.event1")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.NEW)
                    .build();
            repository.save(event1);

            OutboxEventEntity event2 = OutboxEventEntity.builder()
                    .eventType("test.event2")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.NEW)
                    .build();
            repository.save(event2);

            Instant now = Instant.now();
            repository.updateToProcessing(
                    List.of(event1.getId(), event2.getId()),
                    "worker-1",
                    now
            );

            OutboxEventEntity updated1 = repository.findById(event1.getId()).orElseThrow();
            assertThat(updated1.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
            assertThat(updated1.getLockedBy()).isEqualTo("worker-1");
            assertThat(updated1.getLockedAt()).isEqualTo(now);

            OutboxEventEntity updated2 = repository.findById(event2.getId()).orElseThrow();
            assertThat(updated2.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
            assertThat(updated2.getLockedBy()).isEqualTo("worker-1");
        }
    }
}
