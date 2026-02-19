package com.example.interhubdev.notification.internal.infrastructure;

import com.example.interhubdev.notification.internal.application.NotificationRepository;
import com.example.interhubdev.notification.internal.domain.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for NotificationRepository.
 * Tests CRUD operations, unique constraint, pagination, and unread count.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("NotificationRepository")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository repository;

    private static final UUID USER_ID_1 = UUID.randomUUID();
    private static final UUID USER_ID_2 = UUID.randomUUID();
    private static final UUID SOURCE_EVENT_ID_1 = UUID.randomUUID();
    private static final UUID SOURCE_EVENT_ID_2 = UUID.randomUUID();

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("saves notification and generates ID")
        void savesNotification() {
            Notification notification = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);

            Notification saved = repository.save(notification);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getRecipientUserId()).isEqualTo(USER_ID_1);
            assertThat(saved.getTemplateKey()).isEqualTo("test.template");
            assertThat(saved.getSourceEventId()).isEqualTo(SOURCE_EVENT_ID_1);
        }

        @Test
        @DisplayName("throws exception on duplicate (recipientUserId, sourceEventId)")
        void duplicateThrowsException() {
            Notification notification1 = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);
            repository.save(notification1);

            Notification notification2 = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);

            assertThatThrownBy(() -> repository.save(notification2))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("allows same sourceEventId for different recipients")
        void sameSourceEventDifferentRecipients() {
            Notification notification1 = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);
            Notification notification2 = createNotification(USER_ID_2, SOURCE_EVENT_ID_1);

            Notification saved1 = repository.save(notification1);
            Notification saved2 = repository.save(notification2);

            assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
            assertThat(saved1.getRecipientUserId()).isEqualTo(USER_ID_1);
            assertThat(saved2.getRecipientUserId()).isEqualTo(USER_ID_2);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns notification if found")
        void found() {
            Notification notification = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);
            Notification saved = repository.save(notification);

            var found = repository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("returns empty if not found")
        void notFound() {
            var found = repository.findById(UUID.randomUUID());
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByRecipientAndSourceEvent")
    class ExistsByRecipientAndSourceEvent {

        @Test
        @DisplayName("returns true if exists")
        void exists() {
            Notification notification = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);
            repository.save(notification);

            boolean exists = repository.existsByRecipientAndSourceEvent(USER_ID_1, SOURCE_EVENT_ID_1);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("returns false if not exists")
        void notExists() {
            boolean exists = repository.existsByRecipientAndSourceEvent(USER_ID_1, SOURCE_EVENT_ID_1);
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByRecipient")
    class FindByRecipient {

        @Test
        @DisplayName("returns notifications ordered by createdAt DESC")
        void orderedByCreatedAtDesc() throws InterruptedException {
            Notification n1 = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);
            repository.save(n1);
            Thread.sleep(10); // Ensure different timestamps

            Notification n2 = createNotification(USER_ID_1, SOURCE_EVENT_ID_2);
            repository.save(n2);

            List<Notification> notifications = repository.findByRecipient(USER_ID_1, false, null, 10);

            assertThat(notifications).hasSize(2);
            assertThat(notifications.get(0).getSourceEventId()).isEqualTo(SOURCE_EVENT_ID_2); // Newer first
            assertThat(notifications.get(1).getSourceEventId()).isEqualTo(SOURCE_EVENT_ID_1);
        }

        @Test
        @DisplayName("filters unread only when unreadOnly=true")
        void filtersUnreadOnly() {
            Notification unread = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);
            Notification savedUnread = repository.save(unread);

            Notification read = createNotification(USER_ID_1, SOURCE_EVENT_ID_2);
            Notification savedRead = repository.save(read);
            savedRead.markRead(Instant.now());
            repository.save(savedRead);

            List<Notification> all = repository.findByRecipient(USER_ID_1, false, null, 10);
            List<Notification> unreadOnly = repository.findByRecipient(USER_ID_1, true, null, 10);

            assertThat(all).hasSize(2);
            assertThat(unreadOnly).hasSize(1);
            assertThat(unreadOnly.get(0).getId()).isEqualTo(savedUnread.getId());
        }

        @Test
        @DisplayName("respects limit")
        void respectsLimit() {
            for (int i = 0; i < 5; i++) {
                Notification n = createNotification(USER_ID_1, UUID.randomUUID());
                repository.save(n);
            }

            List<Notification> notifications = repository.findByRecipient(USER_ID_1, false, null, 3);

            assertThat(notifications).hasSize(3);
        }

        @Test
        @DisplayName("returns only notifications for specified recipient")
        void filtersByRecipient() {
            Notification n1 = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);
            Notification n2 = createNotification(USER_ID_2, SOURCE_EVENT_ID_2);
            repository.save(n1);
            repository.save(n2);

            List<Notification> user1Notifications = repository.findByRecipient(USER_ID_1, false, null, 10);
            List<Notification> user2Notifications = repository.findByRecipient(USER_ID_2, false, null, 10);

            assertThat(user1Notifications).hasSize(1);
            assertThat(user1Notifications.get(0).getRecipientUserId()).isEqualTo(USER_ID_1);
            assertThat(user2Notifications).hasSize(1);
            assertThat(user2Notifications.get(0).getRecipientUserId()).isEqualTo(USER_ID_2);
        }
    }

    @Nested
    @DisplayName("countUnreadByRecipient")
    class CountUnreadByRecipient {

        @Test
        @DisplayName("returns count of unread notifications")
        void countsUnread() {
            Notification unread1 = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);
            Notification unread2 = createNotification(USER_ID_1, SOURCE_EVENT_ID_2);
            repository.save(unread1);
            repository.save(unread2);

            Notification read = createNotification(USER_ID_1, UUID.randomUUID());
            Notification savedRead = repository.save(read);
            savedRead.markRead(Instant.now());
            repository.save(savedRead);

            long count = repository.countUnreadByRecipient(USER_ID_1);

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("returns 0 if all read")
        void allRead() {
            Notification read = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);
            Notification saved = repository.save(read);
            saved.markRead(Instant.now());
            repository.save(saved);

            long count = repository.countUnreadByRecipient(USER_ID_1);

            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("marks all unread notifications as read")
        void marksAllAsRead() {
            Notification unread1 = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);
            Notification unread2 = createNotification(USER_ID_1, SOURCE_EVENT_ID_2);
            repository.save(unread1);
            repository.save(unread2);

            int updated = repository.markAllAsRead(USER_ID_1, Instant.now());

            assertThat(updated).isEqualTo(2);

            long unreadCount = repository.countUnreadByRecipient(USER_ID_1);
            assertThat(unreadCount).isEqualTo(0);
        }

        @Test
        @DisplayName("does not mark already read notifications")
        void idempotent() {
            Notification unread = createNotification(USER_ID_1, SOURCE_EVENT_ID_1);
            repository.save(unread);

            Notification read = createNotification(USER_ID_1, SOURCE_EVENT_ID_2);
            Notification savedRead = repository.save(read);
            savedRead.markRead(Instant.now());
            repository.save(savedRead);

            int updated = repository.markAllAsRead(USER_ID_1, Instant.now());

            assertThat(updated).isEqualTo(1); // Only unread one
        }
    }

    private Notification createNotification(UUID recipientUserId, UUID sourceEventId) {
        return new Notification(
                recipientUserId,
                "test.template",
                "{\"key\":\"value\"}",
                "{\"route\":\"test\"}",
                sourceEventId,
                "test.event",
                Instant.now()
        );
    }
}
