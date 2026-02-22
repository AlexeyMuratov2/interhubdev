package com.example.interhubdev.submission.internal.archive;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Sanitizes and builds archive and entry filenames.
 * Internal to submission module; designed for reuse or extraction to a shared archiver later.
 */
public final class ArchiveNamingService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int MAX_ARCHIVE_FILENAME_LENGTH = 200;
    private static final int MAX_ENTRY_FILENAME_LENGTH = 200;
    /** Characters forbidden in filenames on common filesystems (Windows, Unix). */
    private static final Pattern FORBIDDEN_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1f]");

    private ArchiveNamingService() {
    }

    /**
     * Build archive filename: subjectName - homeworkTitle - lessonDate.zip
     */
    public static String buildArchiveFilename(ArchiveInfo info) {
        String base = String.join(" - ",
            sanitize(info.subjectName()),
            sanitize(info.homeworkTitle()),
            info.lessonDate().format(DATE_FORMAT)
        );
        return truncate(base, MAX_ARCHIVE_FILENAME_LENGTH - 4) + ".zip";
    }

    /**
     * Build entry filename: studentId - homeworkTitle - studentName - lessonDate[_N].ext
     */
    public static String buildEntryFilename(ArchiveEntry entry) {
        String suffix = entry.fileIndex() > 0 ? "_" + entry.fileIndex() : "";
        String ext = (entry.extension() != null && !entry.extension().isBlank())
            ? entry.extension().startsWith(".") ? entry.extension() : "." + entry.extension()
            : "";
        String base = String.join(" - ",
            entry.studentId().toString(),
            sanitize(entry.homeworkTitle()),
            sanitize(entry.studentName()),
            entry.lessonDate().format(DATE_FORMAT)
        );
        return truncate(base, MAX_ENTRY_FILENAME_LENGTH - suffix.length() - ext.length()) + suffix + ext;
    }

    public static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String s = FORBIDDEN_CHARS.matcher(value).replaceAll(" ");
        s = s.replaceAll("\\s+", " ").trim();
        return s.isEmpty() ? "unnamed" : s;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || s.length() <= maxLen) {
            return s != null ? s : "";
        }
        return s.substring(0, maxLen).trim();
    }
}
