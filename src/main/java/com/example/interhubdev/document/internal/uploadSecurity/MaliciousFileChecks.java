package com.example.interhubdev.document.internal.uploadSecurity;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Checks to reduce risk of malicious file uploads (without antivirus).
 * Detects: path traversal, null bytes, double/masked extensions, reserved names, dangerous extensions,
 * trailing dots/spaces, control characters, filename length.
 */
@Component
class MaliciousFileChecks {

    private static final int MAX_FILENAME_LENGTH = 200;
    /** Allow 1–2 dots (e.g. my.file.pdf); more suggests masking. */
    private static final int MAX_DOTS_IN_FILENAME = 2;
    private static final Pattern PATH_TRAVERSAL = Pattern.compile(".*[\\.]{2}[/\\\\].*|[/\\\\].*");
    private static final Pattern CONTROL_CHARS = Pattern.compile(".*[\\x00-\\x1F\\x7F].*");
    private static final String NULL_BYTE = "\u0000";
    /** Right-to-left override (U+202E) used to mask extensions (e.g. photo\u202Egpj.exe shows as photoexe.jpg). */
    private static final char RTL_OVERRIDE = '\u202E';
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
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
    private static final Set<String> RESERVED_NAMES = Set.of(
        "con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
        "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
    );

    /**
     * Validates filename for common malicious patterns. Throws {@link AppException} via
     * {@link UploadSecurityErrors} if suspicious.
     */
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
        if (countDotsInFilename(originalFilename) > MAX_DOTS_IN_FILENAME) {
            throw UploadSecurityErrors.suspiciousFilename("File name must not use multiple extensions to mask type");
        }
        // Any extension in chain must not be dangerous (catches masking like a.pdf.exe)
        for (String ext : getAllExtensions(originalFilename).toList()) {
            if (DANGEROUS_EXTENSIONS.contains(ext.toLowerCase())) {
                throw UploadSecurityErrors.suspiciousFilename("Executable or script file types are not allowed");
            }
        }
    }

    private static String getBaseName(String filename) {
        int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = lastSlash >= 0 ? filename.substring(lastSlash + 1) : filename;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String getLastExtension(String filename) {
        int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = lastSlash >= 0 ? filename.substring(lastSlash + 1) : filename;
        int lastDot = name.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == name.length() - 1) {
            return null;
        }
        return name.substring(lastDot + 1).trim();
    }

    /** All extensions in filename (e.g. "a.b.pdf" → ["b", "pdf"]). */
    private static Stream<String> getAllExtensions(String filename) {
        int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = lastSlash >= 0 ? filename.substring(lastSlash + 1) : filename;
        int firstDot = name.indexOf('.');
        if (firstDot < 0 || firstDot == name.length() - 1) {
            return Stream.empty();
        }
        return Stream.of(name.substring(firstDot + 1).split("\\."))
            .map(String::trim)
            .filter(s -> !s.isEmpty());
    }

    private static int countDotsInFilename(String filename) {
        int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = lastSlash >= 0 ? filename.substring(lastSlash + 1) : filename;
        return (int) name.chars().filter(ch -> ch == '.').count();
    }
}
