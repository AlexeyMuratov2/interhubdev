package com.example.interhubdev.outbox.internal;

import com.example.interhubdev.outbox.OutboxEvent;
import com.example.interhubdev.outbox.OutboxEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OutboxProcessor.
 * Tests event processing, handler invocation, retry logic, and failure handling.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(OutboxProcessorTest.TestConfig.class)
@TestPropertySource(properties = {
        "outbox.processor.enabled=true",
        "outbox.processor.interval=1000",
        "outbox.processor.batch-size=10",
        "outbox.processor.max-attempts=3",
        "outbox.processor.base-retry-delay-seconds=1",
        "outbox.processor.max-retry-delay-seconds=10",
        "outbox.processor.lock-stale-timeout-seconds=60",
        "outbox.processor.worker-id=test-worker"
})
@DisplayName("OutboxProcessor")
class OutboxProcessorTest {

    @Autowired
    private OutboxEventRepository repository;

    @Autowired
    private OutboxProcessor processor;

    @Autowired
    private OutboxHandlerRegistry handlerRegistry;

    @TestConfiguration
    static class TestConfig {
        @Bean
        OutboxEventHandler testHandler() {
            return new TestEventHandler();
        }
    }

    static class TestEventHandler implements OutboxEventHandler {
        private final AtomicInteger handleCount = new AtomicInteger(0);
        private RuntimeException toThrow = null;

        @Override
        public String eventType() {
            return "test.event";
        }

        @Override
        public void handle(OutboxEvent event) throws Exception {
            handleCount.incrementAndGet();
            if (toThrow != null) {
                throw toThrow;
            }
        }

        int getHandleCount() {
            return handleCount.get();
        }

        void setToThrow(RuntimeException e) {
            this.toThrow = e;
        }
    }

    @Nested
    @DisplayName("processEvents")
    class ProcessEvents {

        @Test
        @DisplayName("processes NEW event successfully")
        void processesNewEvent() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType("test.event")
                    .payloadJson("{\"key\":\"value\"}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.NEW)
                    .attempts(0)
                    .build();
            repository.save(event);

            processor.processEvents();

            OutboxEventEntity updated = repository.findById(event.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.DONE);
            assertThat(updated.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("marks event as FAILED when no handler registered")
        void noHandler() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType("unknown.event")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.NEW)
                    .attempts(0)
                    .build();
            repository.save(event);

            processor.processEvents();

            OutboxEventEntity updated = repository.findById(event.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(updated.getLastError()).contains("No handler registered");
            assertThat(updated.getAttempts()).isEqualTo(1);
            assertThat(updated.getNextRetryAt()).isNotNull();
        }

        @Test
        @DisplayName("retries FAILED event when handler succeeds")
        void retriesFailedEvent() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType("test.event")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.FAILED)
                    .attempts(1)
                    .nextRetryAt(Instant.now().minusSeconds(10))
                    .build();
            repository.save(event);

            processor.processEvents();

            OutboxEventEntity updated = repository.findById(event.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.DONE);
        }

        @Test
        @DisplayName("marks event as permanently FAILED after max attempts")
        void maxAttemptsReached() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType("unknown.event")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.FAILED)
                    .attempts(2) // max-attempts=3, so this is last retry
                    .nextRetryAt(Instant.now().minusSeconds(10))
                    .build();
            repository.save(event);

            processor.processEvents();

            OutboxEventEntity updated = repository.findById(event.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(updated.getAttempts()).isEqualTo(3);
            assertThat(updated.getNextRetryAt()).isNull(); // permanently failed
        }

        @Test
        @DisplayName("handles handler exception and schedules retry")
        void handlerException() {
            TestEventHandler handler = (TestEventHandler) handlerRegistry.getHandler("test.event").orElseThrow();
            handler.setToThrow(new RuntimeException("Handler error"));

            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType("test.event")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.NEW)
                    .attempts(0)
                    .build();
            repository.save(event);

            processor.processEvents();

            OutboxEventEntity updated = repository.findById(event.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(updated.getLastError()).contains("Handler error");
            assertThat(updated.getAttempts()).isEqualTo(1);
            assertThat(updated.getNextRetryAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("releaseStaleLocks")
    class ReleaseStaleLocks {

        @Test
        @DisplayName("releases stale locks before processing")
        void releasesStaleLocks() {
            OutboxEventEntity stale = OutboxEventEntity.builder()
                    .eventType("test.event")
                    .payloadJson("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxEventStatus.PROCESSING)
                    .lockedBy("dead-worker")
                    .lockedAt(Instant.now().minusSeconds(120))
                    .build();
            repository.save(stale);

            processor.processEvents();

            OutboxEventEntity updated = repository.findById(stale.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(updated.getLockedBy()).isNull();
        }
    }
}
