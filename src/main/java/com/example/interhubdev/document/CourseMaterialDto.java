package com.example.interhubdev.document;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for course material (business entity linking group_subject_offering to stored file).
 * Contains material metadata and embedded stored file information.
 * Materials belong to a specific offering, allowing each teacher to have their own materials.
 */
public record CourseMaterialDto(
    UUID id,
    UUID offeringId,
    String title,
    String description,
    UUID authorId,
    LocalDateTime uploadedAt,
    StoredFileDto file
) {
}
