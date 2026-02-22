package com.example.interhubdev.composition;

import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.grades.GradeEntryDto;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.submission.HomeworkSubmissionDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Aggregated homework submissions for a lesson: all students in the lesson's group
 * with their submissions, points, and files per homework assignment.
 * For use on the lesson screen (homework submissions table: students × homeworks).
 */
public record LessonHomeworkSubmissionsDto(
        /**
         * Lesson information (date, time, topic, status).
         */
        LessonDto lesson,

        /**
         * Group that attends this lesson (name, code).
         */
        StudentGroupDto group,

        /**
         * All homework assignments for this lesson (order defines column index for submissions).
         */
        List<HomeworkDto> homeworks,

        /**
         * One row per student in the group; each row has one item per homework (same order as homeworks).
         * If a student did not submit for a homework, the item has null submission and null points.
         */
        List<StudentHomeworkRowDto> studentRows
) {
    /**
     * One row: one student and their submissions for each homework of the lesson.
     */
    public record StudentHomeworkRowDto(
            StudentDto student,
            /**
             * One item per homework (same order as {@link LessonHomeworkSubmissionsDto#homeworks()}).
             * Empty submission means student did not submit; points null means not graded.
             */
            List<StudentHomeworkItemDto> items
    ) {
    }

    /**
     * One cell: one homework — submission (or empty), points, grade entry (if graded), and attached files.
     */
    public record StudentHomeworkItemDto(
            UUID homeworkId,
            /**
             * Submission DTO if student submitted; null otherwise.
             */
            HomeworkSubmissionDto submission,
            /**
             * Points given for this submission (from grades). Null if no submission or not yet graded.
             * When {@link #gradeEntry} is present, points equal gradeEntry.points().
             */
            BigDecimal points,
            /**
             * Grade entry for this submission when one exists (one ACTIVE entry per submission, latest by gradedAt).
             * Null if no submission or not yet graded. Enables frontend to show description and edit by id.
             */
            GradeEntryDto gradeEntry,
            /**
             * File metadata for this submission's attached files (order preserved).
             * Empty if no submission or no files.
             */
            List<StoredFileDto> files
    ) {
    }
}
