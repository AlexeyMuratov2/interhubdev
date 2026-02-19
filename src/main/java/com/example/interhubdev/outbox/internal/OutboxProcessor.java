package com.example.interhubdev.outbox.internal;

import com.example.interhubdev.outbox.OutboxEvent;
import com.example.interhubdev.outbox.OutboxEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Scheduled processor for outbox events.
 * <p>
 * Periodically picks up NEW/FAILED events, locks them atomically,
 * and processes them through registered handlers.
 * <p>
 * Package-private: only accessible within the outbox module.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "outbox.processor.enabled", havingValue = "true", matchIfMissing = true)
class OutboxProcessor {

    private final OutboxEventRepository repository;
    private final OutboxHandlerRegistry handlerRegistry;
    private final OutboxEventMapper mapper;

    @Value("${outbox.processor.batch-size:50}")
    private int batchSize;

    @Value("${outbox.processor.max-attempts:10}")
    private int maxAttempts;

    @Value("${outbox.processor.base-retry-delay-seconds:30}")
    private long baseRetryDelaySeconds;

    @Value("${outbox.processor.max-retry-delay-seconds:1800}")
    private long maxRetryDelaySeconds;

    @Value("${outbox.processor.lock-stale-timeout-seconds:300}")
    private long lockStaleTimeoutSeconds;

    @Value("${outbox.processor.worker-id:default}")
    private String workerId;

    /**
     * Process outbox events.
     * Runs every 10 seconds by default (configurable via outbox.processor.interval).
     */
    @Scheduled(fixedDelayString = "${outbox.processor.interval:10000}")
    @Transactional
    public void processEvents() {
        Instant now = Instant.now();

        // Release stale locks first
        releaseStaleLocks(now);

        // Lock and process batch
        List<OutboxEventEntity> events = lockAndSelectBatch(now);
        if (events.isEmpty()) {
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (OutboxEventEntity entity : events) {
            try {
                processEvent(entity, now);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to process outbox event: id={}, type={}, attempts={}",
                        entity.getId(), entity.getEventType(), entity.getAttempts(), e);
            }
        }

        if (successCount > 0 || failureCount > 0) {
            log.info("Processed outbox batch: {} success, {} failures", successCount, failureCount);
        }
    }

    /**
     * Release stale locks (from dead workers).
     */
    private void releaseStaleLocks(Instant now) {
        Instant staleBefore = now.minusSeconds(lockStaleTimeoutSeconds);
        int released = repository.releaseStaleLocks(staleBefore);
        if (released > 0) {
            log.warn("Released {} stale outbox event locks", released);
        }
    }

    /**
     * Atomically lock and select a batch of events ready for processing.
     */
    private List<OutboxEventEntity> lockAndSelectBatch(Instant now) {
        List<OutboxEventEntity> events = repository.lockNextBatch(batchSize, now);
        if (events.isEmpty()) {
            return events;
        }

        // Update status to PROCESSING in the same transaction
        List<UUID> eventIds = events.stream()
                .map(OutboxEventEntity::getId)
                .toList();
        repository.updateToProcessing(eventIds, workerId, now);

        return events;
    }

    /**
     * Process a single event.
     */
    private void processEvent(OutboxEventEntity entity, Instant now) {
        String eventType = entity.getEventType();
        OutboxEventHandler handler = handlerRegistry.getHandler(eventType)
                .orElse(null);

        if (handler == null) {
            handleNoHandler(entity, now);
            return;
        }

        try {
            OutboxEvent event = mapper.toDto(entity);
            handler.handle(event);
            repository.markDone(entity.getId(), now);
            log.debug("Successfully processed outbox event: id={}, type={}", 
                    entity.getId(), eventType);
        } catch (Exception e) {
            handleProcessingFailure(entity, e, now);
        }
    }

    /**
     * Handle case when no handler is registered for event type.
     */
    private void handleNoHandler(OutboxEventEntity entity, Instant now) {
        int newAttempts = entity.getAttempts() + 1;
        String error = "No handler registered for event type: " + entity.getEventType();

        if (newAttempts >= maxAttempts) {
            // Max attempts reached, mark as permanently failed
            repository.markFailed(entity.getId(), error, newAttempts, null);
            log.error("Outbox event permanently failed (no handler): id={}, type={}, attempts={}",
                    entity.getId(), entity.getEventType(), newAttempts);
        } else {
            // Retry with backoff
            Instant nextRetry = calculateNextRetry(newAttempts);
            repository.markFailed(entity.getId(), error, newAttempts, nextRetry);
            log.warn("Outbox event failed (no handler), will retry: id={}, type={}, attempts={}, nextRetry={}",
                    entity.getId(), entity.getEventType(), newAttempts, nextRetry);
        }
    }

    /**
     * Handle processing failure with retry logic.
     */
    private void handleProcessingFailure(OutboxEventEntity entity, Exception e, Instant now) {
        int newAttempts = entity.getAttempts() + 1;
        String error = e.getMessage();
        if (error == null || error.isBlank()) {
            error = e.getClass().getSimpleName();
        }
        // Truncate error message to avoid huge text fields
        if (error.length() > 1000) {
            error = error.substring(0, 1000) + "...";
        }

        if (newAttempts >= maxAttempts) {
            // Max attempts reached, mark as permanently failed
            repository.markFailed(entity.getId(), error, newAttempts, null);
            log.error("Outbox event permanently failed: id={}, type={}, attempts={}",
                    entity.getId(), entity.getEventType(), newAttempts);
        } else {
            // Retry with exponential backoff
            Instant nextRetry = calculateNextRetry(newAttempts);
            repository.markFailed(entity.getId(), error, newAttempts, nextRetry);
            log.warn("Outbox event failed, will retry: id={}, type={}, attempts={}, nextRetry={}",
                    entity.getId(), entity.getEventType(), newAttempts, nextRetry);
        }
    }

    /**
     * Calculate next retry time using exponential backoff with jitter.
     */
    private Instant calculateNextRetry(int attempts) {
        long baseDelay = baseRetryDelaySeconds;
        long maxDelay = maxRetryDelaySeconds;

        // Exponential backoff: baseDelay * 2^(attempts-1)
        long delay = baseDelay * (1L << (attempts - 1));
        delay = Math.min(delay, maxDelay);

        // Add jitter: random(0, baseDelay)
        long jitter = ThreadLocalRandom.current().nextLong(0, baseDelay);
        delay += jitter;

        return Instant.now().plusSeconds(delay);
    }
}
