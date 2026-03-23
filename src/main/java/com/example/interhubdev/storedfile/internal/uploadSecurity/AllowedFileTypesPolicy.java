package com.example.interhubdev.storedfile.internal.uploadSecurity;

import com.example.interhubdev.storedfile.UploadContextKey;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Policy: allowed vs forbidden file types (MIME and extension).
 */
@Component
class AllowedFileTypesPolicy {

    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
        "exe", "bat", "cmd", "com", "msi", "scr", "jar", "dll", "so", "dylib",
        "vbs", "js", "jse", "ws", "wsf", "wsc", "wsh",
        "ps1", "ps1xml", "ps2", "ps2xml", "psc1", "psc2",
        "msh", "msh1", "msh2", "mshxml", "msh1xml", "msh2xml",
        "sh", "bash", "zsh", "csh", "fish",
        "app", "deb", "rpm", "pkg", "dmg", "apk", "ipa", "bin", "run", "out", "elf"
    );

    private static final Set<String> FORBIDDEN_MIME_TYPES = Set.of(
        "application/x-executable", "application/x-msdownload", "application/x-ms-installer",
        "application/x-sh", "application/x-shellscript", "application/javascript", "application/x-javascript",
        "text/javascript", "application/x-powershell", "application/x-msdos-program", "application/x-bat",
        "application/x-cmd", "application/x-sharedlib", "application/x-elf", "application/java-archive",
        "application/x-java-archive", "application/vnd.android.package-archive", "application/x-debian-package",
        "application/x-redhat-package-manager", "application/x-apple-diskimage", "text/html", "application/xhtml+xml"
    );

    private static final Set<String> ZIP_BASED_MIMES = Set.of(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    void checkAllowed(UploadContextKey contextKey, String contentType, String originalFilename) {
        if (contextKey == UploadContextKey.GENERAL_USER_FILE) {
            return;
        }
        if (contentType == null || contentType.isBlank()) {
            throw UploadSecurityErrors.forbiddenFileType("Content type is required");
        }
        String normalizedMime = contentType.split(";")[0].trim().toLowerCase();
        if (FORBIDDEN_MIME_TYPES.contains(normalizedMime)) {
            throw UploadSecurityErrors.forbiddenFileType("Content type not allowed: " + normalizedMime);
        }
        if (!isSafeMimeType(normalizedMime)) {
            throw UploadSecurityErrors.forbiddenFileType("Content type not allowed: " + normalizedMime);
        }
        String extension = extractExtension(originalFilename);
        if (extension != null && !extension.isEmpty()) {
            if (FORBIDDEN_EXTENSIONS.contains(extension.toLowerCase())) {
                throw UploadSecurityErrors.forbiddenFileType("File extension not allowed: " + extension);
            }
        }
    }

    boolean isZipBasedMime(String normalizedMime) {
        return ZIP_BASED_MIMES.contains(normalizedMime);
    }

    private static boolean isSafeMimeType(String normalizedMime) {
        if (normalizedMime.startsWith("image/") || normalizedMime.startsWith("video/") || normalizedMime.startsWith("audio/")) {
            return true;
        }
        if (normalizedMime.startsWith("text/")) {
            return true;
        }
        if ("application/pdf".equals(normalizedMime)) {
            return true;
        }
        if (normalizedMime.startsWith("application/vnd.")) {
            return true;
        }
        if ("application/msword".equals(normalizedMime) || "application/vnd.ms-excel".equals(normalizedMime)
            || "application/vnd.ms-powerpoint".equals(normalizedMime)) {
            return true;
        }
        if ("application/zip".equals(normalizedMime) || "application/x-zip-compressed".equals(normalizedMime)
            || "application/x-rar-compressed".equals(normalizedMime) || "application/x-rar".equals(normalizedMime)
            || "application/x-7z-compressed".equals(normalizedMime) || "application/x-tar".equals(normalizedMime)
            || "application/x-gzip".equals(normalizedMime) || "application/gzip".equals(normalizedMime)) {
            return true;
        }
        if ("application/json".equals(normalizedMime) || "application/xml".equals(normalizedMime) || "text/xml".equals(normalizedMime)) {
            return true;
        }
        return false;
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
