package com.example.interhubdev.offering;

import java.time.LocalDateTime;
import java.util.UUID;

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
