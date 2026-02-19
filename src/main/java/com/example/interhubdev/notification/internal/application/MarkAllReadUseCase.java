package com.example.interhubdev.notification.internal.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case: mark all notifications as read.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MarkAllReadUseCase {

    private final NotificationRepository repository;

    public void execute(UUID userId) {
        int updated = repository.markAllAsRead(userId, Instant.now());
        log.debug("Marked {} notifications as read for user: userId={}", updated, userId);
    }
}
