package com.example.interhubdev.notification.internal.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA repository for NotificationEntity.
 */
interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    /**
     * Check if notification exists for recipient and source event.
     */
    boolean existsByRecipientUserIdAndSourceEventId(UUID recipientUserId, UUID sourceEventId);

    /**
     * Find notifications for recipient with pagination.
     * <p>
     * Ordered by createdAt DESC, id DESC.
     * For first page: cursorCreatedAt and cursorId are null.
     * For next page: returns notifications where
     * (createdAt < cursorCreatedAt) OR (createdAt = cursorCreatedAt AND id < cursorId).
     */
    @Query("SELECT n FROM NotificationEntity n " +
            "WHERE n.recipientUserId = :recipientUserId " +
            "AND (:unreadOnly = false OR n.readAt IS NULL) " +
            "AND (:cursorCreatedAt IS NULL OR (n.createdAt < :cursorCreatedAt) OR (n.createdAt = :cursorCreatedAt AND n.id < :cursorId)) " +
            "ORDER BY n.createdAt DESC, n.id DESC")
    List<NotificationEntity> findByRecipientUserIdWithPagination(
            @Param("recipientUserId") UUID recipientUserId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("cursorCreatedAt") java.time.Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Count unread notifications for recipient.
     */
    long countByRecipientUserIdAndReadAtIsNull(UUID recipientUserId);

    /**
     * Mark all notifications as read for recipient.
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.readAt = :readAt " +
            "WHERE n.recipientUserId = :recipientUserId AND n.readAt IS NULL")
    int markAllAsRead(@Param("recipientUserId") UUID recipientUserId, @Param("readAt") Instant readAt);
}
