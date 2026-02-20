package com.example.interhubdev.document.internal.uploadSecurity;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Policy: blacklist-based file type validation.
 * Allows safe file types (images, videos, audio, documents, archives) and blocks only dangerous ones.
 * Single source of truth for "what file types we reject" in the upload security layer.
 */
@Component
class AllowedFileTypesPolicy {

    /**
     * Forbidden file extensions: executables, scripts, and other potentially dangerous files.
     * This list is checked against the file extension to prevent dangerous uploads.
     */
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
        // Executables
        "exe", "bat", "cmd", "com", "msi", "scr", "jar", "dll", "so", "dylib",
        // Scripts
        "vbs", "js", "jse", "ws", "wsf", "wsc", "wsh",
        "ps1", "ps1xml", "ps2", "ps2xml", "psc1", "psc2",
        "msh", "msh1", "msh2", "mshxml", "msh1xml", "msh2xml",
        "sh", "bash", "zsh", "csh", "fish",
        // Other dangerous
        "app", "deb", "rpm", "pkg", "dmg", "apk", "ipa",
        "bin", "run", "out", "elf"
    );

    /**
     * Forbidden MIME types: executables, scripts, and other potentially dangerous content types.
     */
    private static final Set<String> FORBIDDEN_MIME_TYPES = Set.of(
        "application/x-executable",
        "application/x-msdownload",
        "application/x-ms-installer",
        "application/x-sh",
        "application/x-shellscript",
        "application/javascript",
        "application/x-javascript",
        "text/javascript",
        "application/x-powershell",
        "application/x-msdos-program",
        "application/x-bat",
        "application/x-cmd",
        "application/x-sharedlib",
        "application/x-elf",
        "application/java-archive",
        "application/x-java-archive",
        "application/vnd.android.package-archive",
        "application/x-debian-package",
        "application/x-redhat-package-manager",
        "application/x-apple-diskimage",
        "text/html", // XSS risk
        "application/xhtml+xml" // XSS risk
    );

    /** MIMEs that are ZIP-based (docx, xlsx, pptx); magic bytes detect application/zip. */
    private static final Set<String> ZIP_BASED_MIMES = Set.of(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    /**
     * Checks that content type and extension are not in the forbidden list.
     * Allows all safe file types: images (image/*), videos (video/*), audio (audio/*),
     * text files (text/*), documents (application/pdf, Office formats), and safe archives.
     * Throws {@link AppException} via {@link UploadSecurityErrors} on violation.
     */
    void checkAllowed(String contentType, String originalFilename) {
        if (contentType == null || contentType.isBlank()) {
            throw UploadSecurityErrors.forbiddenFileType("Content type is required");
        }
        String normalizedMime = contentType.split(";")[0].trim().toLowerCase();

        // Check if MIME type is explicitly forbidden
        if (FORBIDDEN_MIME_TYPES.contains(normalizedMime)) {
            throw UploadSecurityErrors.forbiddenFileType("Content type not allowed: " + normalizedMime);
        }

        // Check if MIME type category is safe
        if (!isSafeMimeType(normalizedMime)) {
            throw UploadSecurityErrors.forbiddenFileType("Content type not allowed: " + normalizedMime);
        }

        // Check file extension
        String extension = extractExtension(originalFilename);
        if (extension != null && !extension.isEmpty()) {
            String extLower = extension.toLowerCase();
            if (FORBIDDEN_EXTENSIONS.contains(extLower)) {
                throw UploadSecurityErrors.forbiddenFileType("File extension not allowed: " + extension);
            }
        }
    }

    /**
     * Checks if MIME type is safe (allowed).
     * Safe categories:
     * - image/* (all images)
     * - video/* (all videos)
     * - audio/* (all audio)
     * - text/* (all text files, except HTML which is in forbidden list)
     * - application/pdf
     * - application/msword and Office formats (application/vnd.*)
     * - Safe archives (application/zip, application/x-rar-compressed, etc.)
     */
    private static boolean isSafeMimeType(String normalizedMime) {
        // Images: allow all
        if (normalizedMime.startsWith("image/")) {
            return true;
        }
        // Videos: allow all
        if (normalizedMime.startsWith("video/")) {
            return true;
        }
        // Audio: allow all
        if (normalizedMime.startsWith("audio/")) {
            return true;
        }
        // Text files: allow all (HTML is explicitly forbidden in FORBIDDEN_MIME_TYPES)
        if (normalizedMime.startsWith("text/")) {
            return true;
        }
        // PDF documents
        if ("application/pdf".equals(normalizedMime)) {
            return true;
        }
        // Office documents (old and new formats)
        if (normalizedMime.startsWith("application/vnd.")) {
            return true;
        }
        if ("application/msword".equals(normalizedMime) ||
            "application/vnd.ms-excel".equals(normalizedMime) ||
            "application/vnd.ms-powerpoint".equals(normalizedMime)) {
            return true;
        }
        // Safe archives
        if ("application/zip".equals(normalizedMime) ||
            "application/x-zip-compressed".equals(normalizedMime) ||
            "application/x-rar-compressed".equals(normalizedMime) ||
            "application/x-rar".equals(normalizedMime) ||
            "application/x-7z-compressed".equals(normalizedMime) ||
            "application/x-tar".equals(normalizedMime) ||
            "application/x-gzip".equals(normalizedMime) ||
            "application/gzip".equals(normalizedMime)) {
            return true;
        }
        // JSON, XML (safe data formats)
        if ("application/json".equals(normalizedMime) ||
            "application/xml".equals(normalizedMime) ||
            "text/xml".equals(normalizedMime)) {
            return true;
        }
        return false;
    }

    /** True if declared MIME is Office OpenXML (ZIP-based); magic bytes will detect application/zip. */
    boolean isZipBasedMime(String normalizedMime) {
        return ZIP_BASED_MIMES.contains(normalizedMime);
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
