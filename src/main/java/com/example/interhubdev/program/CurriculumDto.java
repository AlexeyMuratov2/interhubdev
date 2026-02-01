package com.example.interhubdev.program;

import java.time.LocalDateTime;
import java.util.UUID;

public record CurriculumDto(
    UUID id,
    UUID programId,
    String version,
    int startYear,
    boolean isActive,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
