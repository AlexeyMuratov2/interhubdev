package com.example.interhubdev.document;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * DTO for homework assignment (linked to a lesson, optional file).
 */
public record HomeworkDto(
    UUID id,
    UUID lessonId,
    String title,
    String description,
    Integer points,
    Optional<StoredFileDto> file,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
