package com.example.interhubdev.notification.internal.infrastructure;

import com.example.interhubdev.notification.internal.application.NotificationRepository;
import com.example.interhubdev.notification.internal.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationRepository using JPA.
 */
@Repository
@RequiredArgsConstructor
class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = NotificationMappers.toEntity(notification);
        NotificationEntity saved = jpaRepository.save(entity);
        return NotificationMappers.toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(NotificationMappers::toDomain);
    }

    @Override
    public boolean existsByRecipientAndSourceEvent(UUID recipientUserId, UUID sourceEventId) {
        return jpaRepository.existsByRecipientUserIdAndSourceEventId(recipientUserId, sourceEventId);
    }

    @Override
    public List<Notification> findByRecipient(UUID recipientUserId, boolean unreadOnly, UUID cursor, int limit) {
        PageRequest pageable = PageRequest.of(0, limit);
        List<NotificationEntity> entities;

        if (cursor != null) {
            NotificationEntity cursorEntity = jpaRepository.findById(cursor)
                    .orElseThrow(() -> new IllegalArgumentException("Cursor notification not found: " + cursor));
            entities = jpaRepository.findNextPage(
                    recipientUserId, unreadOnly,
                    cursorEntity.getCreatedAt(), cursorEntity.getId(),
                    pageable);
        } else {
            entities = jpaRepository.findFirstPage(recipientUserId, unreadOnly, pageable);
        }

        return entities.stream()
                .map(NotificationMappers::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countUnreadByRecipient(UUID recipientUserId) {
        return jpaRepository.countByRecipientUserIdAndReadAtIsNull(recipientUserId);
    }

    @Override
    public int markAllAsRead(UUID recipientUserId, java.time.Instant readAt) {
        return jpaRepository.markAllAsRead(recipientUserId, readAt);
    }
}
