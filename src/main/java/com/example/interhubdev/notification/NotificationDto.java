package com.example.interhubdev.notification;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for notification.
 * <p>
 * Contains template key, params, and data for client-side localization and navigation.
 * Params and data are plain maps so that they serialize to JSON objects correctly in API responses.
 */
public record NotificationDto(
        UUID id,
        String templateKey,
        Map<String, Object> params,
        Map<String, Object> data,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant readAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant archivedAt
) {
}
