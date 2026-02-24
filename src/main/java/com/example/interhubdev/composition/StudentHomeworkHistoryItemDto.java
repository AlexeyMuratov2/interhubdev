package com.example.interhubdev.composition;

import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.grades.GradeEntryDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.submission.HomeworkSubmissionDto;

import java.util.List;
import java.util.Optional;

/**
 * One homework assignment in student homework history: full homework and lesson context,
 * optional student submission with grade and file metadata. For frontend to render the full row.
 */
public record StudentHomeworkHistoryItemDto(
        /**
         * Homework assignment (title, description, points, attached files, lessonId).
         */
        HomeworkDto homework,

        /**
         * Lesson this homework belongs to (date, time, topic, status). For ordering and display.
         */
        LessonDto lesson,

        /**
         * Student's submission for this homework, if any. At most one per student per homework (latest if replaced).
         */
        Optional<HomeworkSubmissionDto> submission,

        /**
         * Grade entry for this submission when one exists (ACTIVE, latest by gradedAt). Empty if no submission or not graded.
         */
        Optional<GradeEntryDto> gradeEntry,

        /**
         * File metadata for this submission's attached files (order preserved). Empty if no submission or no files.
         */
        List<StoredFileDto> submissionFiles
) {
}
