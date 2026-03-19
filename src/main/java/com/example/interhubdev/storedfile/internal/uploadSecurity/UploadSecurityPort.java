package com.example.interhubdev.storedfile.internal.uploadSecurity;

import com.example.interhubdev.error.AppException;

import java.nio.file.Path;

/**
 * Port for upload security: size, filename, MIME, magic bytes, antivirus.
 */
public interface UploadSecurityPort {

    /**
     * Ensure upload is allowed. Call before storage. Throws AppException on rejection.
     *
     * @param context     upload context
     * @param contentPath  path to temp file for content scan (magic bytes, antivirus); null to skip
     * @throws AppException if not allowed
     */
    void ensureUploadAllowed(UploadContext context, Path contentPath);
}
