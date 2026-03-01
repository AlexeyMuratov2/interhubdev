package com.example.interhubdev.composition;

import com.example.interhubdev.absencenotice.StudentNoticeSummaryDto;
import com.example.interhubdev.attendancerecord.AttendanceStatus;
import com.example.interhubdev.attendance.SessionAttendanceDto;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.student.StudentDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregated roster attendance for a lesson: all students in the group with attendance status
 * and absence notices for this lesson. For use on the lesson screen (attendance table UI).
 */
public record LessonRosterAttendanceDto(
        /**
         * Lesson information (date, time, topic, status).
         */
        LessonDto lesson,

        /**
         * Group information (name, code).
         */
        StudentGroupDto group,

        /**
         * Subject name for UI header (e.g. "Mathematics").
         */
        String subjectName,

        /**
         * Counts by attendance status (PRESENT, ABSENT, LATE, EXCUSED).
         */
        Map<AttendanceStatus, Integer> counts,

        /**
         * Number of students with no attendance record yet.
         */
        int unmarkedCount,

        /**
         * One row per student in the group: student display data + attendance + notices.
         * Order matches group roster.
         */
        List<LessonRosterAttendanceRowDto> rows
) {
    /**
     * Single row in the lesson roster attendance table: student info + attendance record + absence notices.
     */
    public record LessonRosterAttendanceRowDto(
            /**
             * Student display data (id, studentId, chineseName, faculty, course, etc.).
             */
            StudentDto student,

            /**
             * Attendance status for this lesson. Null if not yet marked.
             */
            AttendanceStatus status,

            /**
             * Minutes late (only when status is LATE).
             */
            Integer minutesLate,

            /**
             * Teacher comment on the attendance record.
             */
            String teacherComment,

            /**
             * When attendance was marked.
             */
            LocalDateTime markedAt,

            /**
             * User ID who marked attendance.
             */
            UUID markedBy,

            /**
             * Absence notice attached to this record (if any).
             */
            Optional<UUID> attachedAbsenceNoticeId,

            /**
             * All absence notices for this student and lesson (for UI: type, status, reason, submittedAt, fileIds).
             */
            List<StudentNoticeSummaryDto> notices,

            /**
             * Points for this lesson only (ACTIVE entries with lesson_id = this lesson and no homework submission).
             * Homework points are separate. Zero if no lesson-only grades.
             */
            BigDecimal lessonPoints
    ) {
    }
}
