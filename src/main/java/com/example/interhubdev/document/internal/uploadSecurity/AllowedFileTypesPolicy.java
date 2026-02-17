package com.example.interhubdev.document.internal.uploadSecurity;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Policy: allowed MIME types and their permitted file extensions.
 * Single source of truth for "what file types we accept" in the upload security layer.
 */
@Component
class AllowedFileTypesPolicy {

    private static final Map<String, Set<String>> MIME_TO_EXTENSIONS = Map.ofEntries(
        Map.entry("application/pdf", Set.of("pdf")),
        Map.entry("application/msword", Set.of("doc")),
        Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", Set.of("docx")),
        Map.entry("application/vnd.ms-excel", Set.of("xls")),
        Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Set.of("xlsx")),
        Map.entry("text/plain", Set.of("txt", "log")),
        Map.entry("text/csv", Set.of("csv")),
        Map.entry("image/jpeg", Set.of("jpg", "jpeg")),
        Map.entry("image/png", Set.of("png")),
        Map.entry("image/gif", Set.of("gif")),
        Map.entry("image/webp", Set.of("webp"))
    );

    /** MIMEs that are ZIP-based (docx, xlsx); magic bytes detect application/zip. */
    private static final Set<String> ZIP_BASED_MIMES = Set.of(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    /** All allowed MIME types (base type only, no params). */
    Set<String> getAllowedMimeTypes() {
        return MIME_TO_EXTENSIONS.keySet();
    }

    /** True if declared MIME is Office OpenXML (ZIP-based); magic bytes will detect application/zip. */
    boolean isZipBasedMime(String normalizedMime) {
        return ZIP_BASED_MIMES.contains(normalizedMime);
    }

    /**
     * Checks that contentType is allowed and, if filename has an extension, that it matches the content type.
     * Throws {@link AppException} via {@link UploadSecurityErrors} on violation.
     */
    void checkAllowed(String contentType, String originalFilename) {
        if (contentType == null || contentType.isBlank()) {
            throw UploadSecurityErrors.forbiddenFileType("Content type is required");
        }
        String normalizedMime = contentType.split(";")[0].trim().toLowerCase();
        if (!MIME_TO_EXTENSIONS.containsKey(normalizedMime)) {
            throw UploadSecurityErrors.forbiddenFileType("Content type not allowed: " + normalizedMime);
        }
        String extension = extractExtension(originalFilename);
        if (extension != null && !extension.isEmpty()) {
            Set<String> allowed = MIME_TO_EXTENSIONS.get(normalizedMime);
            if (!allowed.contains(extension.toLowerCase())) {
                throw UploadSecurityErrors.extensionMismatch(
                    "File extension '" + extension + "' does not match content type " + normalizedMime);
            }
        }
    }

    private static String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDot + 1).trim();
    }
}
