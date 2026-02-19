package com.example.interhubdev.attendance;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for attendance records of a lesson session.
 */
public record SessionAttendanceDto(
        UUID sessionId,
        Map<AttendanceStatus, Integer> counts,
        Integer unmarkedCount,
        List<SessionAttendanceStudentDto> students
) {
    /**
     * Student attendance record in session context.
     */
    public record SessionAttendanceStudentDto(
            UUID studentId,
            AttendanceStatus status, // UNMARKED represented as null or special handling
            Integer minutesLate,
            String teacherComment,
            java.time.LocalDateTime markedAt,
            UUID markedBy,
            /**
             * Optional link to absence notice attached to this record.
             */
            java.util.Optional<UUID> absenceNoticeId,
            /**
             * List of absence notices for this student and session (for UI display).
             * Minimal DTO with essential fields.
             */
            List<StudentNoticeDto> notices
    ) {
    }

    /**
     * Minimal DTO for absence notice in session context (for UI display in student row).
     */
    public record StudentNoticeDto(
            UUID id,
            AbsenceNoticeType type,
            AbsenceNoticeStatus status,
            java.util.Optional<String> reasonText,
            java.time.LocalDateTime submittedAt,
            List<String> fileIds
    ) {
    }
}
