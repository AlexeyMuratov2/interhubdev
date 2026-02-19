package com.example.interhubdev.notification.internal.application;

import com.example.interhubdev.notification.internal.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case: create notification from outbox event.
 * <p>
 * Handles idempotency: if notification already exists for (recipientUserId, sourceEventId),
 * silently ignores (treats as success).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreateNotificationUseCase {

    private final NotificationRepository repository;

    /**
     * Create notification. Idempotent: if duplicate, returns without error.
     *
     * @param notification notification to create
     * @return true if created, false if already exists (idempotent)
     */
    public boolean execute(Notification notification) {
        try {
            repository.save(notification);
            log.debug("Created notification: id={}, recipient={}, templateKey={}, sourceEventId={}",
                    notification.getId(), notification.getRecipientUserId(),
                    notification.getTemplateKey(), notification.getSourceEventId());
            return true;
        } catch (DataIntegrityViolationException e) {
            // Unique constraint violation: notification already exists (idempotent)
            log.debug("Notification already exists (idempotent): recipient={}, sourceEventId={}",
                    notification.getRecipientUserId(), notification.getSourceEventId());
            return false;
        }
    }
}
