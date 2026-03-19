package com.example.interhubdev.storedfile.internal.uploadSecurity;

import java.util.UUID;

/**
 * Context for upload security check (technical only: no purpose or roles).
 */
public record UploadContext(
    UUID uploadedBy,
    String contentType,
    long size,
    String originalFilename
) {
    public static UploadContext of(UUID uploadedBy, String contentType, long size, String originalFilename) {
        return new UploadContext(
            uploadedBy != null ? uploadedBy : null,
            contentType != null ? contentType : "",
            size,
            originalFilename != null ? originalFilename : ""
        );
    }
}
