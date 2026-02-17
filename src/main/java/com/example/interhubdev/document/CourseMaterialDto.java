package com.example.interhubdev.document;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for course material (business entity linking subject to stored file).
 * Contains material metadata and embedded stored file information.
 */
public record CourseMaterialDto(
    UUID id,
    UUID subjectId,
    String title,
    String description,
    UUID authorId,
    LocalDateTime uploadedAt,
    StoredFileDto file
) {
}
