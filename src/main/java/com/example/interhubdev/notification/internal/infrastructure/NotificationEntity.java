package com.example.interhubdev.notification.internal.infrastructure;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for notification table.
 */
@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_recipient_read_created", columnList = "recipient_user_id,read_at,created_at"),
        @Index(name = "idx_notification_recipient_created", columnList = "recipient_user_id,created_at"),
        @Index(name = "idx_notification_source_event_id", columnList = "source_event_id"),
        @Index(name = "idx_notification_template_key_created", columnList = "template_key,created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "template_key", nullable = false, length = 255)
    private String templateKey;

    @Column(name = "params_json", nullable = false, columnDefinition = "JSONB")
    private String paramsJson;

    @Column(name = "data_json", nullable = false, columnDefinition = "JSONB")
    private String dataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "source_event_id", nullable = false)
    private UUID sourceEventId;

    @Column(name = "source_event_type", nullable = false, length = 255)
    private String sourceEventType;

    @Column(name = "source_occurred_at", nullable = false)
    private Instant sourceOccurredAt;
}
