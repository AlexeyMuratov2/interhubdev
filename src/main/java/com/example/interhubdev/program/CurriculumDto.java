package com.example.interhubdev.program;

import java.time.LocalDateTime;
import java.util.UUID;

public record CurriculumDto(
    UUID id,
    UUID programId,
    String version,
    int startYear,
    Integer endYear,
    boolean isActive,
    CurriculumStatus status,
    LocalDateTime approvedAt,
    UUID approvedBy,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
