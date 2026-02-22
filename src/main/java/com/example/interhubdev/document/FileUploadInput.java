package com.example.interhubdev.document;

import java.nio.file.Path;

/**
 * Input for a single file in a batch upload. Used by {@link DocumentApi#uploadFiles}.
 * Caller is responsible for deleting the temp file after the call.
 *
 * @param tempPath        path to temp file (must exist, readable)
 * @param originalFilename original file name
 * @param contentType     MIME type
 * @param size            file size in bytes
 */
public record FileUploadInput(
    Path tempPath,
    String originalFilename,
    String contentType,
    long size
) {
}
