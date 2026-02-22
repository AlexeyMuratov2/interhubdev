package com.example.interhubdev.document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for homework assignment (linked to a lesson, optional list of files).
 */
public record HomeworkDto(
    UUID id,
    UUID lessonId,
    String title,
    String description,
    Integer points,
    List<StoredFileDto> files,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
