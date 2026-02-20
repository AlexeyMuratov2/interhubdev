package com.example.interhubdev.outbox.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for outbox_event table with atomic locking support.
 * Package-private: only accessible within the outbox module.
 */
@Repository
interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * Atomically lock and retrieve a batch of events ready for processing.
     * <p>
     * Uses PostgreSQL FOR UPDATE SKIP LOCKED to safely handle concurrent workers.
     * Only selects events with status NEW or FAILED that are ready for retry
     * (next_retry_at is null or <= now).
     * <p>
     * This method must be called within a transaction. The selected rows are locked
     * and should be immediately updated to PROCESSING status using updateToProcessing.
     *
     * @param limit maximum number of events to lock
     * @param now current timestamp
     * @return list of locked events (must be updated to PROCESSING in same transaction)
     */
    @Query(value = """
        SELECT * FROM outbox_event
        WHERE status IN ('NEW', 'FAILED')
        AND (next_retry_at IS NULL OR next_retry_at <= :now)
        ORDER BY occurred_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEventEntity> lockNextBatch(
            @Param("limit") int limit,
            @Param("now") Instant now
    );

    /**
     * Mark event as done (successfully processed).
     *
     * @param eventId event ID
     * @param processedAt processing timestamp
     */
    @Modifying
    @Query("""
        UPDATE OutboxEventEntity e
        SET e.status = 'DONE',
            e.processedAt = :processedAt,
            e.lockedBy = NULL,
            e.lockedAt = NULL
        WHERE e.id = :eventId
        """)
    void markDone(@Param("eventId") UUID eventId, @Param("processedAt") Instant processedAt);

    /**
     * Mark event as failed and schedule retry.
     *
     * @param eventId event ID
     * @param error error message
     * @param attempts new attempt count
     * @param nextRetryAt when to retry
     */
    @Modifying
    @Query("""
        UPDATE OutboxEventEntity e
        SET e.status = 'FAILED',
            e.lastError = :error,
            e.attempts = :attempts,
            e.nextRetryAt = :nextRetryAt,
            e.lockedBy = NULL,
            e.lockedAt = NULL
        WHERE e.id = :eventId
        """)
    void markFailed(
            @Param("eventId") UUID eventId,
            @Param("error") String error,
            @Param("attempts") int attempts,
            @Param("nextRetryAt") Instant nextRetryAt
    );

    /**
     * Release stale locks (events locked by workers that died).
     * Resets status to FAILED so they can be retried.
     *
     * @param staleBefore timestamp threshold: locks older than this are considered stale
     * @return number of locks released
     */
    @Modifying
    @Query("""
        UPDATE OutboxEventEntity e
        SET e.status = 'FAILED',
            e.lockedBy = NULL,
            e.lockedAt = NULL
        WHERE e.status = 'PROCESSING'
        AND e.lockedAt < :staleBefore
        """)
    int releaseStaleLocks(@Param("staleBefore") Instant staleBefore);

    /**
     * Update event status to PROCESSING and set lock fields.
     * Called immediately after lockNextBatch in the same transaction.
     *
     * @param eventIds IDs of events to update (from lockNextBatch)
     * @param workerId identifier of the worker instance
     * @param now current timestamp
     */
    @Modifying
    @Query(value = """
        UPDATE outbox_event
        SET status = 'PROCESSING',
            locked_by = :workerId,
            locked_at = :now
        WHERE id IN (:eventIds)
        """, nativeQuery = true)
    void updateToProcessing(
            @Param("eventIds") List<UUID> eventIds,
            @Param("workerId") String workerId,
            @Param("now") Instant now
    );

    /**
     * Count events by status (for monitoring).
     */
    long countByStatus(OutboxEventStatus status);

    /**
     * Find event by ID (for testing).
     */
    Optional<OutboxEventEntity> findById(UUID id);
}
