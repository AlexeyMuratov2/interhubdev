package com.example.interhubdev.document;

import java.util.Set;
import java.util.UUID;

/**
 * Context for upload security check: who uploads and what file metadata is known before storage.
 * Used by {@link UploadSecurityPort} to decide whether the upload is allowed.
 *
 * <p>Optional {@link #purpose()} and {@link #roles()} support scenario-based policy (e.g. COURSE_MATERIAL
 * vs HOMEWORK_SUBMISSION). For MVP, generic baseline policy applies when purpose is not set.
 */
public record UploadContext(
    UUID uploadedBy,
    String contentType,
    long size,
    String originalFilename,
    UploadPurpose purpose,
    Set<String> roles
) {
    /**
     * Creates context for security check with generic baseline policy.
     */
    public static UploadContext of(UUID uploadedBy, String contentType, long size, String originalFilename) {
        return new UploadContext(uploadedBy, contentType, size, originalFilename != null ? originalFilename : "", null, Set.of());
    }

    /**
     * Creates context with optional purpose and roles for scenario-based policy.
     */
    public static UploadContext of(UUID uploadedBy, String contentType, long size, String originalFilename,
            UploadPurpose purpose, Set<String> roles) {
        return new UploadContext(uploadedBy, contentType, size, originalFilename != null ? originalFilename : "",
            purpose, roles != null ? roles : Set.of());
    }
}
