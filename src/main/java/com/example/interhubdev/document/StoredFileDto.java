package com.example.interhubdev.document;

import com.example.interhubdev.storedfile.FileStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for stored file metadata (size, content type, upload info).
 * Note: storagePath is internal and not exposed to clients.
 * Only files with status ACTIVE may be bound to business entities or downloaded.
 */
public record StoredFileDto(
    UUID id,
    long size,
    String contentType,
    String originalName,
    LocalDateTime uploadedAt,
    UUID uploadedBy,
    FileStatus status
) {
    /** True if file is available for bind and download (activation gate passed). */
    public boolean isActive() {
        return status == FileStatus.ACTIVE;
    }
}
