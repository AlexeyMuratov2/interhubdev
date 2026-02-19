package com.example.interhubdev.notification.internal.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case: get unread notification count.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUnreadCountUseCase {

    private final NotificationRepository repository;

    public long execute(UUID userId) {
        return repository.countUnreadByRecipient(userId);
    }
}
