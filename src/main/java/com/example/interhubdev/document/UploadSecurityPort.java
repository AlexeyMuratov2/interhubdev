package com.example.interhubdev.document;

import com.example.interhubdev.error.AppException;

import java.nio.file.Path;

/**
 * Port for upload security: answers "are we allowed to accept this file?"
 * Implemented by the upload security layer inside the document module.
 * Runs before storage and validation; throws {@link AppException} on rejection.
 *
 * <p>When {@code contentPath} is provided, full scan (magic bytes + antivirus) is performed.
 * When {@code contentPath} is null, only metadata checks run (filename, MIME, size).
 */
public interface UploadSecurityPort {

    /**
     * Ensures the upload is allowed by policy (file type, extension, malicious patterns, size, antivirus).
     * Call this at the start of the upload flow, before storage and format validation.
     *
     * @param context    upload context (user, contentType, size, filename)
     * @param contentPath path to temp file for content scan (antivirus, magic bytes); null to skip content checks
     * @throws AppException if upload is not allowed (forbidden type, suspicious filename, malware, etc.)
     */
    void ensureUploadAllowed(UploadContext context, Path contentPath);
}
