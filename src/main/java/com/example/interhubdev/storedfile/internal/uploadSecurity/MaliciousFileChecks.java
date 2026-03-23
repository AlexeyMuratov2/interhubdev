package com.example.interhubdev.storedfile.internal.uploadSecurity;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks for malicious filename patterns (path traversal, double extension, etc.).
 */
@Component
class MaliciousFileChecks {

    private static final int MAX_FILENAME_LENGTH = 200;
    private static final Pattern PATH_TRAVERSAL = Pattern.compile(".*[\\.]{2}[/\\\\].*|[/\\\\].*");
    private static final Pattern CONTROL_CHARS = Pattern.compile(".*[\\x00-\\x1F\\x7F].*");
    private static final String NULL_BYTE = "\u0000";
    private static final char RTL_OVERRIDE = '\u202E';
    private static final Set<String> RESERVED_NAMES = Set.of(
        "con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
        "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
    );

    void checkFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw UploadSecurityErrors.suspiciousFilename("File name is required");
        }
        if (originalFilename.length() > MAX_FILENAME_LENGTH) {
            throw UploadSecurityErrors.suspiciousFilename("File name must not exceed " + MAX_FILENAME_LENGTH + " characters");
        }
        if (originalFilename.contains(NULL_BYTE)) {
            throw UploadSecurityErrors.suspiciousFilename("File name contains invalid characters");
        }
        if (originalFilename.indexOf(RTL_OVERRIDE) >= 0) {
            throw UploadSecurityErrors.suspiciousFilename("File name contains invalid characters");
        }
        if (originalFilename.contains("..")) {
            throw UploadSecurityErrors.suspiciousFilename("File name must not contain consecutive dots");
        }
        if (CONTROL_CHARS.matcher(originalFilename).matches()) {
            throw UploadSecurityErrors.suspiciousFilename("File name contains invalid characters");
        }
        if (originalFilename.endsWith(".") || originalFilename.endsWith(" ") || originalFilename.endsWith("\t")) {
            throw UploadSecurityErrors.suspiciousFilename("File name must not end with dot or whitespace");
        }
        if (PATH_TRAVERSAL.matcher(originalFilename).matches()) {
            throw UploadSecurityErrors.suspiciousFilename("File name must not contain path separators or traversal");
        }
        String base = getBaseName(originalFilename);
        if (RESERVED_NAMES.contains(base.toLowerCase())) {
            throw UploadSecurityErrors.suspiciousFilename("File name is not allowed");
        }
    }

    private static String getBaseName(String filename) {
        int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = lastSlash >= 0 ? filename.substring(lastSlash + 1) : filename;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
