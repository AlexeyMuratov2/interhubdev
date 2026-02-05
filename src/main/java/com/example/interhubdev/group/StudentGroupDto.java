package com.example.interhubdev.group;

import java.time.LocalDateTime;
import java.util.UUID;

public record StudentGroupDto(
    UUID id,
    UUID programId,
    UUID curriculumId,
    String code,
    String name,
    String description,
    int startYear,
    Integer graduationYear,
    UUID curatorUserId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
