package com.example.interhubdev.composition;

import com.example.interhubdev.attendance.AttendanceRecordDto;
import com.example.interhubdev.attendance.StudentNoticeSummaryDto;
import com.example.interhubdev.schedule.LessonDto;

import java.util.List;
import java.util.Optional;

/**
 * One lesson row in student attendance history: lesson info + student's attendance and absence notices.
 * For frontend to render the full history table.
 */
public record StudentAttendanceHistoryLessonItemDto(
        /**
         * Lesson data (date, time, topic, status, room, etc.).
         */
        LessonDto lesson,

        /**
         * Student's attendance record for this lesson, if marked.
         */
        Optional<AttendanceRecordDto> attendance,

        /**
         * All absence notices submitted by the student for this lesson (for display).
         */
        List<StudentNoticeSummaryDto> absenceNotices
) {
}
