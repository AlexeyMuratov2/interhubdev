package com.example.interhubdev.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for notification.
 * <p>
 * Contains template key, params, and data for client-side localization and navigation.
 */
public record NotificationDto(
        UUID id,
        String templateKey,
        JsonNode params,
        JsonNode data,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant readAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant archivedAt
) {
}
