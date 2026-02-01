package com.example.interhubdev.program;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProgramDto(
    UUID id,
    String code,
    String name,
    String description,
    String degreeLevel,
    UUID departmentId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
