package com.example.interhubdev.subject;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubjectDto(
    UUID id,
    String code,
    String chineseName,
    String englishName,
    String description,
    UUID departmentId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
