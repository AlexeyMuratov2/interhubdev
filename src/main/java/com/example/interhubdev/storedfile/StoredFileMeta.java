package com.example.interhubdev.storedfile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for stored file metadata. Storage path is internal and not exposed.
 * Only ACTIVE files are available for bind/download (see FileStatus).
 */
public record StoredFileMeta(
    UUID id,
    long size,
    String contentType,
    String originalName,
    LocalDateTime uploadedAt,
    UUID uploadedBy,
    FileStatus status,
    FileSafetyClass safetyClass
) {
}
