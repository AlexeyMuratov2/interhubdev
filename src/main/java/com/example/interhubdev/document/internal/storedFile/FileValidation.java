package com.example.interhubdev.document.internal.storedFile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Validates file upload: size, content type, and name.
 * Throws {@link AppException} via {@link DocumentErrors} on violation.
 */
@Component
class FileValidation {

    private static final int MAX_ORIGINAL_NAME_LENGTH = 500;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain",
        "text/csv",
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp"
    );

    @Value("${app.document.max-file-size-bytes:52428800}")
    private long maxFileSizeBytes;

    /**
     * Validate file for upload. Throws AppException (via DocumentErrors) if invalid.
     */
    void validateUpload(long size, String contentType, String originalFilename) {
        if (size <= 0) {
            throw DocumentErrors.invalidFileType("File size must be positive");
        }
        if (size > maxFileSizeBytes) {
            throw DocumentErrors.fileTooLarge(maxFileSizeBytes);
        }
        if (contentType == null || contentType.isBlank()) {
            throw DocumentErrors.invalidFileType("Content type is required");
        }
        String normalizedType = contentType.split(";")[0].trim().toLowerCase();
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedType)) {
            throw DocumentErrors.invalidFileType("Content type not allowed: " + contentType);
        }
        if (originalFilename == null || originalFilename.isBlank()) {
            throw DocumentErrors.invalidFileName("File name is required");
        }
        if (originalFilename.length() > MAX_ORIGINAL_NAME_LENGTH) {
            throw DocumentErrors.invalidFileName("File name must not exceed " + MAX_ORIGINAL_NAME_LENGTH + " characters");
        }
    }
}
