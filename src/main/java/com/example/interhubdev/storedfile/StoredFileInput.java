package com.example.interhubdev.storedfile;

import java.nio.file.Path;

/**
 * Input for a single file in a batch upload.
 *
 * @param tempPath         path to temp file (caller deletes after)
 * @param originalFilename original file name
 * @param contentType      MIME type
 * @param size             file size in bytes
 */
public record StoredFileInput(
    Path tempPath,
    String originalFilename,
    String contentType,
    long size
) {
}
