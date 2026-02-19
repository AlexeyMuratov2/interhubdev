/**
 * Outbox module - infrastructure for reliable integration event delivery.
 * <p>
 * Provides transactional outbox pattern implementation for publishing integration events
 * that are guaranteed to be delivered even if the publishing transaction commits successfully.
 * <p>
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.outbox.OutboxIntegrationEventPublisher} - interface for publishing events</li>
 *   <li>{@link com.example.interhubdev.outbox.OutboxEventDraft} - builder for event metadata</li>
 *   <li>{@link com.example.interhubdev.outbox.OutboxEventHandler} - interface for event handlers</li>
 * </ul>
 * <p>
 * <h2>Usage</h2>
 * <p>
 * To publish an event from a domain module:
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class MyService {
 *     private final OutboxIntegrationEventPublisher publisher;
 *     
 *     @Transactional
 *     public void doSomething() {
 *         // ... business logic ...
 *         
 *         // Publish event in the same transaction
 *         publisher.publish("attendance.absence_notice_submitted", Map.of(
 *             "absenceNoticeId", id,
 *             "studentId", studentId
 *         ));
 *     }
 * }
 * }</pre>
 * <p>
 * To register an event handler:
 * <pre>{@code
 * @Component
 * public class MyEventHandler implements OutboxEventHandler {
 *     @Override
 *     public String eventType() {
 *         return "attendance.absence_notice_submitted";
 *     }
 *     
 *     @Override
 *     public void handle(OutboxEvent event) {
 *         // Process event
 *     }
 * }
 * }</pre>
 * <p>
 * <h2>How it works</h2>
 * <ol>
 *   <li>Publisher writes event to outbox_event table in the same transaction</li>
 *   <li>OutboxProcessor (scheduled job) picks up NEW/FAILED events</li>
 *   <li>Processor locks events atomically (FOR UPDATE SKIP LOCKED)</li>
 *   <li>Processor finds handler by event_type and calls handle()</li>
 *   <li>On success: event marked as DONE</li>
 *   <li>On failure: event marked as FAILED with exponential backoff retry</li>
 * </ol>
 * <p>
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>error - for error handling (if needed)</li>
 * </ul>
 * <p>
 * This is an infrastructure module. Domain modules depend on it, but it does not depend on domain modules.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Outbox",
    allowedDependencies = {"error"}
)
package com.example.interhubdev.outbox;
