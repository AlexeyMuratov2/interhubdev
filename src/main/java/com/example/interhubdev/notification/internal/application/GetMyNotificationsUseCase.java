package com.example.interhubdev.notification.internal.application;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.notification.NotificationApi;
import com.example.interhubdev.notification.NotificationDto;
import com.example.interhubdev.notification.NotificationPage;
import com.example.interhubdev.notification.internal.domain.Notification;
import com.example.interhubdev.notification.internal.infrastructure.NotificationMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Use case: get my notifications with pagination.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetMyNotificationsUseCase {

    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_LIMIT = 30;

    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;

    public NotificationPage execute(UUID userId, String statusFilter, UUID cursor, int limit) {
        // Validate statusFilter
        boolean unreadOnly;
        if (statusFilter == null || statusFilter.equalsIgnoreCase("all")) {
            unreadOnly = false;
        } else if (statusFilter.equalsIgnoreCase("unread")) {
            unreadOnly = true;
        } else {
            throw Errors.badRequest("Invalid statusFilter: " + statusFilter + ". Must be 'all' or 'unread'");
        }

        // Cap limit
        int cappedLimit = Math.min(Math.max(1, limit), MAX_LIMIT);
        // Fetch limit+1 to check if more exists
        int fetchLimit = cappedLimit + 1;

        List<Notification> notifications = repository.findByRecipient(userId, unreadOnly, cursor, fetchLimit);

        boolean hasMore = notifications.size() > cappedLimit;
        List<Notification> pageNotifications = hasMore
                ? notifications.subList(0, cappedLimit)
                : notifications;

        UUID nextCursor = hasMore && !pageNotifications.isEmpty()
                ? pageNotifications.get(pageNotifications.size() - 1).getId()
                : null;

        List<NotificationDto> dtos = pageNotifications.stream()
                .map(n -> NotificationMappers.toDto(n, objectMapper))
                .toList();

        return new NotificationPage(dtos, nextCursor);
    }
}
