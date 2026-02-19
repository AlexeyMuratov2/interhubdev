package com.example.interhubdev.notification.internal.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity for notification.
 * <p>
 * Represents a notification in the user's inbox.
 * Domain rules: templateKey must not be blank; markRead is idempotent.
 */
public class Notification {

    private UUID id;
    private UUID recipientUserId;
    private String templateKey;
    private String paramsJson;
    private String dataJson;
    private Instant createdAt;
    private Instant readAt;
    private Instant archivedAt;
    private UUID sourceEventId;
    private String sourceEventType;
    private Instant sourceOccurredAt;

    // Constructor for creating new notification
    public Notification(
            UUID recipientUserId,
            String templateKey,
            String paramsJson,
            String dataJson,
            UUID sourceEventId,
            String sourceEventType,
            Instant sourceOccurredAt) {
        if (templateKey == null || templateKey.isBlank()) {
            throw new IllegalArgumentException("Template key must not be blank");
        }
        this.recipientUserId = recipientUserId;
        this.templateKey = templateKey;
        this.paramsJson = paramsJson;
        this.dataJson = dataJson;
        this.sourceEventId = sourceEventId;
        this.sourceEventType = sourceEventType;
        this.sourceOccurredAt = sourceOccurredAt;
        this.createdAt = Instant.now();
    }

    // Constructor for loading from DB
    public Notification(
            UUID id,
            UUID recipientUserId,
            String templateKey,
            String paramsJson,
            String dataJson,
            Instant createdAt,
            Instant readAt,
            Instant archivedAt,
            UUID sourceEventId,
            String sourceEventType,
            Instant sourceOccurredAt) {
        this.id = id;
        this.recipientUserId = recipientUserId;
        this.templateKey = templateKey;
        this.paramsJson = paramsJson;
        this.dataJson = dataJson;
        this.createdAt = createdAt;
        this.readAt = readAt;
        this.archivedAt = archivedAt;
        this.sourceEventId = sourceEventId;
        this.sourceEventType = sourceEventType;
        this.sourceOccurredAt = sourceOccurredAt;
    }

    /**
     * Mark notification as read.
     * Idempotent: if already read, does nothing.
     *
     * @param when timestamp when marked as read
     */
    public void markRead(Instant when) {
        if (this.readAt == null) {
            this.readAt = when;
        }
    }

    /**
     * Archive notification.
     *
     * @param when timestamp when archived
     */
    public void archive(Instant when) {
        if (this.archivedAt == null) {
            this.archivedAt = when;
        }
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public String getParamsJson() {
        return paramsJson;
    }

    public String getDataJson() {
        return dataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public String getSourceEventType() {
        return sourceEventType;
    }

    public Instant getSourceOccurredAt() {
        return sourceOccurredAt;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }
}
