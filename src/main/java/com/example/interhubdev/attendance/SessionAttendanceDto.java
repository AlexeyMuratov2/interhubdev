package com.example.interhubdev.attendance;

import com.example.interhubdev.absencenotice.StudentNoticeSummaryDto;
import com.example.interhubdev.attendancerecord.AttendanceStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * DTO for attendance records of a lesson session (merged: records + notices per student).
 */
public record SessionAttendanceDto(
        UUID sessionId,
        Map<AttendanceStatus, Integer> counts,
        Integer unmarkedCount,
        List<SessionAttendanceStudentDto> students
) {
    /**
     * Student row: attendance record + list of absence notices for this student and session.
     */
    public record SessionAttendanceStudentDto(
            UUID studentId,
            AttendanceStatus status,
            Integer minutesLate,
            String teacherComment,
            LocalDateTime markedAt,
            UUID markedBy,
            Optional<UUID> absenceNoticeId,
            List<StudentNoticeSummaryDto> notices
    ) {
    }
}
