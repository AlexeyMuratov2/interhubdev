package com.example.interhubdev.storedfile.internal.uploadSecurity;

import com.example.interhubdev.storedfile.UploadContextKey;

import java.util.UUID;

/**
 * Context for upload security check (technical only: no purpose or roles).
 */
public record UploadContext(
    UUID uploadedBy,
    UploadContextKey contextKey,
    String contentType,
    long size,
    String originalFilename
) {
    public static UploadContext of(
        UUID uploadedBy,
        UploadContextKey contextKey,
        String contentType,
        long size,
        String originalFilename
    ) {
        return new UploadContext(
            uploadedBy != null ? uploadedBy : null,
            contextKey,
            contentType != null ? contentType : "",
            size,
            originalFilename != null ? originalFilename : ""
        );
    }
}
