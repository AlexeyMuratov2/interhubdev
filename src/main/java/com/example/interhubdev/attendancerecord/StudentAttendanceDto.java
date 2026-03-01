package com.example.interhubdev.attendancerecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for student attendance history.
 */
public record StudentAttendanceDto(
        UUID studentId,
        LocalDateTime from,
        LocalDateTime to,
        Map<AttendanceStatus, Integer> summary,
        Integer totalMarked,
        List<StudentAttendanceRecordDto> records
) {
    /**
     * Single attendance record in student history context.
     */
    public record StudentAttendanceRecordDto(
            UUID lessonSessionId,
            AttendanceStatus status,
            Integer minutesLate,
            String teacherComment,
            LocalDateTime markedAt
    ) {
    }
}
