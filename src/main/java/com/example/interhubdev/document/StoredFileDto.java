package com.example.interhubdev.document;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for stored file metadata (size, content type, upload info).
 * Note: storagePath is internal and not exposed to clients.
 */
public record StoredFileDto(
    UUID id,
    long size,
    String contentType,
    String originalName,
    LocalDateTime uploadedAt,
    UUID uploadedBy
) {
}
