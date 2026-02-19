package com.example.interhubdev.notification;

import java.util.UUID;

/**
 * Public API for Notification module.
 * <p>
 * Provides operations for retrieving and managing user notifications.
 * All errors are thrown as {@link com.example.interhubdev.error.AppException} (via Errors),
 * handled by global exception handler as ErrorResponse.
 */
public interface NotificationApi {

    /**
     * Get notifications for the current user with cursor-based pagination.
     * <p>
     * Notifications are ordered by created_at DESC (newest first).
     *
     * @param userId       user ID (must be authenticated user)
     * @param statusFilter filter by read status: "all" for all notifications, "unread" for unread only
     * @param cursor       optional cursor (last notification id from previous page); null for first page
     * @param limit        max items per page (will be capped at 50)
     * @return page with notifications and optional next cursor
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if statusFilter invalid
     */
    NotificationPage getMyNotifications(UUID userId, String statusFilter, UUID cursor, int limit);

    /**
     * Get unread notification count for the user.
     *
     * @param userId user ID (must be authenticated user)
     * @return unread count (0 or positive)
     */
    long getUnreadCount(UUID userId);

    /**
     * Mark a notification as read.
     * <p>
     * Idempotent: if already read, does nothing.
     *
     * @param userId         user ID (must be authenticated user)
     * @param notificationId notification ID
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if notification not found,
     *                                                     FORBIDDEN if notification does not belong to user
     */
    void markAsRead(UUID userId, UUID notificationId);

    /**
     * Mark all notifications as read for the user.
     * <p>
     * Idempotent: if all already read, does nothing.
     *
     * @param userId user ID (must be authenticated user)
     */
    void markAllAsRead(UUID userId);
}
