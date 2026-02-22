package com.example.interhubdev.submission.internal.archive;

import java.time.LocalDate;

/**
 * Metadata for the submissions archive (used for archive filename and context).
 * Internal to submission module (public for cross-package use within module).
 */
public record ArchiveInfo(
    String subjectName,
    String homeworkTitle,
    LocalDate lessonDate
) {
}
