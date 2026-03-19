package com.example.interhubdev.storedfile.internal;

import org.springframework.stereotype.Component;

/**
 * Validates file upload: size and filename. Used after upload security checks.
 */
@Component
class FileValidation {

    void validateUpload(long size, String contentType, String originalFilename) {
        if (size <= 0) {
            throw StoredFileErrors.invalidFile("File size must be positive");
        }
        if (originalFilename == null || originalFilename.isBlank()) {
            throw StoredFileErrors.invalidFile("File name is required");
        }
    }
}
