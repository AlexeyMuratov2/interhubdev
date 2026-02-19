package com.example.interhubdev.notification.internal.infrastructure;

import com.example.interhubdev.notification.NotificationDto;
import com.example.interhubdev.notification.internal.domain.Notification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

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

    /**
     * Convert domain Notification to DTO.
     */
    public static NotificationDto toDto(Notification domain, ObjectMapper objectMapper) {
        try {
            JsonNode paramsNode = objectMapper.readTree(domain.getParamsJson());
            JsonNode dataNode = objectMapper.readTree(domain.getDataJson());
            return new NotificationDto(
                    domain.getId(),
                    domain.getTemplateKey(),
                    paramsNode,
                    dataNode,
                    domain.getCreatedAt(),
                    domain.getReadAt(),
                    domain.getArchivedAt()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse JSON in notification: " + domain.getId(), e);
        }
    }
}
