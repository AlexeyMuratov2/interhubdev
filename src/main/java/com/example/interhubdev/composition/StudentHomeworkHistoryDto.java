package com.example.interhubdev.composition;

import com.example.interhubdev.student.StudentDto;

import java.util.List;
import java.util.UUID;

/**
 * Full homework history for a student in an offering: all homework assignments of the offering
 * and the student's solution (submission) and grade per assignment. For frontend to display
 * complete student homework info (e.g. student card).
 */
public record StudentHomeworkHistoryDto(
        /**
         * Student display data.
         */
        StudentDto student,

        /**
         * Offering ID (for context).
         */
        UUID offeringId,

        /**
         * Subject name for header (e.g. "Mathematics").
         */
        String subjectName,

        /**
         * All homeworks for the offering with student's submission and grade per homework.
         * Order: by lesson date and start time, then by homework creation.
         */
        List<StudentHomeworkHistoryItemDto> items
) {
}
