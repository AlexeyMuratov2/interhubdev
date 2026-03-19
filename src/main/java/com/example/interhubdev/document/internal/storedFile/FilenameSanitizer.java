package com.example.interhubdev.document.internal.storedFile;

/**
 * Sanitizes original filename for safe use in HTTP headers (e.g. Content-Disposition), UI, and logs.
 * Prevents path traversal, control characters, and unsafe characters. Original filename is untrusted input.
 */
public final class FilenameSanitizer {

    private static final int MAX_DISPLAY_LENGTH = 255;
    private static final String FALLBACK = "download";

    private FilenameSanitizer() {
    }

    /**
     * Sanitize filename for Content-Disposition and other response headers. Not for storage path.
     *
     * @param originalName client-provided filename (untrusted)
     * @return safe filename for headers, or fallback if empty/invalid
     */
    public static String sanitizeForContentDisposition(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return FALLBACK;
        }
        String s = originalName
            .replace("\\", "_")
            .replace("/", "_")
            .replace("\0", "")
            .replaceAll("[\\x00-\\x1F\\x7F]", "_");
        if (s.length() > MAX_DISPLAY_LENGTH) {
            s = s.substring(0, MAX_DISPLAY_LENGTH);
        }
        s = s.trim();
        return s.isEmpty() ? FALLBACK : s;
    }
}
