package com.example.interhubdev.web;

/**
 * Sanitizes untrusted filenames for safe use in HTTP headers.
 */
public final class FilenameSanitizer {

    private static final int MAX_DISPLAY_LENGTH = 255;
    private static final String FALLBACK = "download";

    private FilenameSanitizer() {
    }

    public static String sanitizeForContentDisposition(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return FALLBACK;
        }
        String sanitized = originalName
            .replace("\\", "_")
            .replace("/", "_")
            .replace("\0", "")
            .replaceAll("[\\x00-\\x1F\\x7F]", "_");
        if (sanitized.length() > MAX_DISPLAY_LENGTH) {
            sanitized = sanitized.substring(0, MAX_DISPLAY_LENGTH);
        }
        sanitized = sanitized.trim();
        return sanitized.isEmpty() ? FALLBACK : sanitized;
    }
}
