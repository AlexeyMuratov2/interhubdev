package com.example.interhubdev.notification.internal.application;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.notification.internal.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case: mark notification as read.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MarkNotificationReadUseCase {

    private final NotificationRepository repository;

    public void execute(UUID userId, UUID notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> Errors.notFound("Notification not found: " + notificationId));

        // Check ownership
        if (!notification.getRecipientUserId().equals(userId)) {
            throw Errors.forbidden("Notification does not belong to user");
        }

        // Mark as read (idempotent)
        notification.markRead(Instant.now());
        repository.save(notification);

        log.debug("Marked notification as read: id={}, userId={}", notificationId, userId);
    }
}
