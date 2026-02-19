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
            UUID markedBy
    ) {
    }
}
