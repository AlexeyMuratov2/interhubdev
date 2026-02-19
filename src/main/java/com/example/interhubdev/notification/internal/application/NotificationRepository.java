package com.example.interhubdev.notification.internal.application;

import com.example.interhubdev.notification.internal.domain.Notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for notification repository operations.
 * <p>
 * Application layer interface for persistence operations.
 */
public interface NotificationRepository {

    /**
     * Save notification. If notification with same (recipientUserId, sourceEventId) exists,
     * throws exception (idempotency handled at DB level via unique constraint).
     *
     * @param notification notification to save
     * @return saved notification with generated ID
     * @throws org.springframework.dao.DataIntegrityViolationException if duplicate (should be caught and handled as idempotent)
     */
    Notification save(Notification notification);

    /**
     * Find notification by ID.
     *
     * @param id notification ID
     * @return optional notification if found
     */
    Optional<Notification> findById(UUID id);

    /**
     * Check if notification exists for recipient and source event (for idempotency check).
     *
     * @param recipientUserId recipient user ID
     * @param sourceEventId   source event ID
     * @return true if exists
     */
    boolean existsByRecipientAndSourceEvent(UUID recipientUserId, UUID sourceEventId);

    /**
     * Find notifications for user with pagination.
     * <p>
     * Ordered by created_at DESC (newest first).
     *
     * @param recipientUserId recipient user ID
     * @param unreadOnly      if true, only return unread notifications
     * @param cursor          optional cursor (last notification id from previous page); null for first page
     * @param limit           max items to return (should fetch limit+1 to check if more exists)
     * @return list of notifications (may be empty)
     */
    List<Notification> findByRecipient(UUID recipientUserId, boolean unreadOnly, UUID cursor, int limit);

    /**
     * Count unread notifications for user.
     *
     * @param recipientUserId recipient user ID
     * @return unread count
     */
    long countUnreadByRecipient(UUID recipientUserId);

    /**
     * Mark all notifications as read for user.
     *
     * @param recipientUserId recipient user ID
     * @param readAt          timestamp when marked as read
     * @return number of notifications updated
     */
    int markAllAsRead(UUID recipientUserId, Instant readAt);
}
