package com.example.interhubdev.notification.internal;

import com.example.interhubdev.notification.NotificationApi;
import com.example.interhubdev.notification.NotificationPage;
import com.example.interhubdev.notification.internal.application.GetMyNotificationsUseCase;
import com.example.interhubdev.notification.internal.application.GetUnreadCountUseCase;
import com.example.interhubdev.notification.internal.application.MarkAllReadUseCase;
import com.example.interhubdev.notification.internal.application.MarkNotificationReadUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of NotificationApi.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class NotificationServiceImpl implements NotificationApi {

    private final GetMyNotificationsUseCase getMyNotificationsUseCase;
    private final GetUnreadCountUseCase getUnreadCountUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final MarkAllReadUseCase markAllReadUseCase;

    @Override
    public NotificationPage getMyNotifications(UUID userId, String statusFilter, UUID cursor, int limit) {
        return getMyNotificationsUseCase.execute(userId, statusFilter, cursor, limit);
    }

    @Override
    public long getUnreadCount(UUID userId) {
        return getUnreadCountUseCase.execute(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        markNotificationReadUseCase.execute(userId, notificationId);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        markAllReadUseCase.execute(userId);
    }
}
