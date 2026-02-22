package com.example.interhubdev.submission;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Handle for streaming a submissions ZIP archive. Allows setting response headers
 * (using {@link #getFilename()}) before writing the archive content to the response stream.
 */
public interface SubmissionsArchiveHandle {

    /**
     * Suggested filename for Content-Disposition (e.g. "Subject - Homework - 2025-02-21.zip").
     * Safe for attachment header after sanitizing (e.g. escape quotes).
     */
    String getFilename();

    /**
     * Write the ZIP archive to the given output stream. Caller must not close the stream.
     *
     * @param out output stream (e.g. response output stream)
     * @throws IOException if writing fails
     */
    void writeTo(OutputStream out) throws IOException;
}
