package com.example.interhubdev.composition;

import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.grades.GradeEntryDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.user.UserDto;

import java.util.Optional;

/**
 * One grade entry with full context for display: lesson (if grade is for a lesson),
 * homework and submission (if grade is for homework), and who graded.
 */
public record StudentGradeHistoryItemDto(
    GradeEntryDto gradeEntry,
    /** Present when this grade is linked to a lesson (e.g. seminar points). */
    Optional<LessonDto> lesson,
    /** Present when this grade is linked to a homework submission. */
    Optional<HomeworkDto> homework,
    /** Present when this grade is linked to a homework submission. */
    Optional<HomeworkSubmissionDto> submission,
    /** User who created or last updated this grade (for display "Graded by X"). */
    Optional<UserDto> gradedByUser
) {
}
