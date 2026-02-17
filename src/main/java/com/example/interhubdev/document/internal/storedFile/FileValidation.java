package com.example.interhubdev.document.internal.storedFile;

import org.springframework.stereotype.Component;

/**
 * Validates file upload: size > 0 and filename presence.
 * Max size and filename checks are enforced by the upload security layer
 * ({@link com.example.interhubdev.document.internal.uploadSecurity.UploadSecurityService}).
 * Throws {@link AppException} via {@link DocumentErrors} on violation.
 */
@Component
class FileValidation {

    /**
     * Validate file size and name for upload. Throws AppException (via DocumentErrors) if invalid.
     * Call after {@link com.example.interhubdev.document.UploadSecurityPort#ensureUploadAllowed}.
     */
    void validateUpload(long size, String contentType, String originalFilename) {
        if (size <= 0) {
            throw DocumentErrors.invalidFileType("File size must be positive");
        }
        if (originalFilename == null || originalFilename.isBlank()) {
            throw DocumentErrors.invalidFileName("File name is required");
        }
    }
}
