package com.example.interhubdev.notification.internal.infrastructure.handlers;

import com.example.interhubdev.notification.NotificationContentResolver;
import com.example.interhubdev.notification.ResolvedNotificationContent;
import com.example.interhubdev.notification.ResolvedNotificationItem;
import com.example.interhubdev.notification.internal.application.CreateNotificationUseCase;
import com.example.interhubdev.notification.internal.domain.Notification;
import com.example.interhubdev.outbox.OutboxEvent;
import com.example.interhubdev.outbox.OutboxEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handler for submission.homework_submission.submitted event.
 * <p>
 * Delegates content resolution to {@link NotificationContentResolver} (implemented in adapter);
 * creates one notification per resolved item (e.g. per teacher of the lesson).
 */
@Component
@RequiredArgsConstructor
@Slf4j
class HomeworkSubmissionSubmittedHandler implements OutboxEventHandler {

    private static final String EVENT_TYPE = "submission.homework_submission.submitted";

    private final NotificationContentResolver notificationContentResolver;
    private final CreateNotificationUseCase createNotificationUseCase;
    private final ObjectMapper objectMapper;

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        var contentOpt = notificationContentResolver.resolve(eventType(), event.getPayload());
        if (contentOpt.isEmpty()) {
            log.warn("NotificationContentResolver returned empty for eventType={}", eventType());
            return;
        }
        ResolvedNotificationContent content = contentOpt.get();
        for (ResolvedNotificationItem item : content.items()) {
            String paramsJson = objectMapper.writeValueAsString(item.params());
            String dataJson = objectMapper.writeValueAsString(item.data());
            Notification notification = new Notification(
                    item.recipientUserId(),
                    item.templateKey(),
                    paramsJson,
                    dataJson,
                    event.getId(),
                    event.getEventType(),
                    content.sourceOccurredAt()
            );
            createNotificationUseCase.execute(notification);
        }
        log.info("Created {} notification(s) for event: eventType={}, eventId={}",
                content.items().size(), eventType(), event.getId());
    }
}
