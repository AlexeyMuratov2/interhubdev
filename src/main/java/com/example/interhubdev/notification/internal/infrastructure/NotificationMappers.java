package com.example.interhubdev.notification.internal.infrastructure;

import com.example.interhubdev.notification.NotificationDto;
import com.example.interhubdev.notification.internal.domain.Notification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Mappers for converting between domain, entity, and DTO.
 */
public final class NotificationMappers {

    private NotificationMappers() {
        // Utility class
    }

    /**
     * Convert domain Notification to JPA entity.
     */
    static NotificationEntity toEntity(Notification domain) {
        return NotificationEntity.builder()
                .id(domain.getId())
                .recipientUserId(domain.getRecipientUserId())
                .templateKey(domain.getTemplateKey())
                .paramsJson(domain.getParamsJson())
                .dataJson(domain.getDataJson())
                .createdAt(domain.getCreatedAt())
                .readAt(domain.getReadAt())
                .archivedAt(domain.getArchivedAt())
                .sourceEventId(domain.getSourceEventId())
                .sourceEventType(domain.getSourceEventType())
                .sourceOccurredAt(domain.getSourceOccurredAt())
                .build();
    }

    /**
     * Convert JPA entity to domain Notification.
     */
    static Notification toDomain(NotificationEntity entity) {
        return new Notification(
                entity.getId(),
                entity.getRecipientUserId(),
                entity.getTemplateKey(),
                entity.getParamsJson(),
                entity.getDataJson(),
                entity.getCreatedAt(),
                entity.getReadAt(),
                entity.getArchivedAt(),
                entity.getSourceEventId(),
                entity.getSourceEventType(),
                entity.getSourceOccurredAt()
        );
    }

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    /**
     * Convert domain Notification to DTO.
     * Parses params_json and data_json into maps so they serialize as plain JSON objects in API responses.
     */
    public static NotificationDto toDto(Notification domain, ObjectMapper objectMapper) {
        try {
            Map<String, Object> params = parseJsonToMap(domain.getParamsJson(), objectMapper);
            Map<String, Object> data = parseJsonToMap(domain.getDataJson(), objectMapper);
            return new NotificationDto(
                    domain.getId(),
                    domain.getTemplateKey(),
                    params,
                    data,
                    domain.getCreatedAt(),
                    domain.getReadAt(),
                    domain.getArchivedAt()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse JSON in notification: " + domain.getId(), e);
        }
    }

    private static Map<String, Object> parseJsonToMap(String json, ObjectMapper objectMapper) throws IOException {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        return objectMapper.readValue(json, MAP_TYPE);
    }
}
