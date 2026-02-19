package com.example.interhubdev.outbox;

/**
 * Handler for processing outbox events.
 * <p>
 * Implement this interface and register as a Spring component to handle
 * specific event types.
 * <p>
 * Example:
 * <pre>{@code
 * @Component
 * @RequiredArgsConstructor
 * public class AbsenceNoticeHandler implements OutboxEventHandler {
 *     private final NotificationService notificationService;
 *     
 *     @Override
 *     public String eventType() {
 *         return "attendance.absence_notice_submitted";
 *     }
 *     
 *     @Override
 *     public void handle(OutboxEvent event) {
 *         Map<String, Object> payload = event.getPayload();
 *         UUID absenceNoticeId = UUID.fromString(payload.get("absenceNoticeId").toString());
 *         notificationService.sendAbsenceNoticeNotification(absenceNoticeId);
 *     }
 * }
 * }</pre>
 */
public interface OutboxEventHandler {

    /**
     * Return the event type this handler processes.
     * Must match exactly the eventType used when publishing.
     *
     * @return event type identifier
     */
    String eventType();

    /**
     * Process the event.
     * <p>
     * If this method throws an exception, the event will be marked as FAILED
     * and retried according to the retry policy.
     *
     * @param event the outbox event to process
     * @throws Exception if processing fails (will trigger retry)
     */
    void handle(OutboxEvent event) throws Exception;
}
