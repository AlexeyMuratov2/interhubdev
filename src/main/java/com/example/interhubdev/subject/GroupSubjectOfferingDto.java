package com.example.interhubdev.subject;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for group subject offering (from offering module).
 * Used in subject module's ports to avoid direct dependency on offering module's DTO.
 */
public record GroupSubjectOfferingDto(
    UUID id,
    UUID groupId,
    UUID curriculumSubjectId,
    UUID teacherId,
    UUID roomId,
    String format,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
