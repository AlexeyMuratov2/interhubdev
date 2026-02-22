package com.example.interhubdev.submission.internal.archive;

import java.util.UUID;

/**
 * One file entry to include in the submissions ZIP.
 * Internal to submission module.
 *
 * @param studentId     student (author) UUID
 * @param studentName   display name of student
 * @param homeworkTitle title of the homework
 * @param lessonDate    date of the lesson
 * @param storedFileId  document module stored file ID
 * @param originalName  original filename (for extension fallback)
 * @param extension     file extension to use in entry name (e.g. "pdf")
 * @param fileIndex     index among files of the same submission (for uniqueness when multiple files per student)
 */
public record ArchiveEntry(
    UUID studentId,
    String studentName,
    String homeworkTitle,
    java.time.LocalDate lessonDate,
    UUID storedFileId,
    String originalName,
    String extension,
    int fileIndex
) {
}
