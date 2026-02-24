package com.example.interhubdev.composition;

import com.example.interhubdev.student.StudentDto;

import java.util.List;
import java.util.UUID;

/**
 * Full attendance history for a student in an offering: all lessons with attendance and absence notices.
 * Includes summary counts for UI (missed lessons, total absence notices submitted).
 */
public record StudentAttendanceHistoryDto(
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
         * Number of lessons where student was marked ABSENT or EXCUSED (missed).
         */
        int missedCount,

        /**
         * Total number of absence notices submitted by the student for this offering's lessons.
         */
        int absenceNoticesSubmittedCount,

        /**
         * All lessons for the offering (any status) with student's attendance and notices per lesson.
         * Order: by lesson date and start time.
         */
        List<StudentAttendanceHistoryLessonItemDto> lessons
) {
}
