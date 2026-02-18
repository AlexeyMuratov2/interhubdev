package com.example.interhubdev.document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for lesson material (business entity linking a lesson to stored files).
 * One lesson has many materials; one material has many files. Contains material metadata
 * and ordered list of stored file DTOs.
 */
public record LessonMaterialDto(
    UUID id,
    UUID lessonId,
    String name,
    String description,
    UUID authorId,
    LocalDateTime publishedAt,
    List<StoredFileDto> files
) {
}
