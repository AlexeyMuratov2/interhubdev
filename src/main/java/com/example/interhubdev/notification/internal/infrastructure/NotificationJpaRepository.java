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
     * Find first page of notifications for recipient (no cursor).
     * Ordered by createdAt DESC, id DESC.
     */
    @Query("SELECT n FROM NotificationEntity n " +
            "WHERE n.recipientUserId = :recipientUserId " +
            "AND (:unreadOnly = false OR n.readAt IS NULL) " +
            "ORDER BY n.createdAt DESC, n.id DESC")
    List<NotificationEntity> findFirstPage(
            @Param("recipientUserId") UUID recipientUserId,
            @Param("unreadOnly") boolean unreadOnly,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Find next page of notifications for recipient using cursor.
     * Returns notifications where (createdAt &lt; cursorCreatedAt)
     * OR (createdAt = cursorCreatedAt AND id &lt; cursorId).
     * Ordered by createdAt DESC, id DESC.
     */
    @Query("SELECT n FROM NotificationEntity n " +
            "WHERE n.recipientUserId = :recipientUserId " +
            "AND (:unreadOnly = false OR n.readAt IS NULL) " +
            "AND ((n.createdAt < :cursorCreatedAt) OR (n.createdAt = :cursorCreatedAt AND n.id < :cursorId)) " +
            "ORDER BY n.createdAt DESC, n.id DESC")
    List<NotificationEntity> findNextPage(
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
