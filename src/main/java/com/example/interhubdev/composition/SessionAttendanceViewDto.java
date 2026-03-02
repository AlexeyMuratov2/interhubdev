package com.example.interhubdev.composition;

import com.example.interhubdev.absencenotice.StudentNoticeSummaryDto;
import com.example.interhubdev.attendancerecord.AttendanceStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only view DTO for attendance records of a lesson session (merged: records + notices per student).
 * For use by composition layer and UI.
 */
public record SessionAttendanceViewDto(
        UUID sessionId,
        Map<AttendanceStatus, Integer> counts,
        Integer unmarkedCount,
        List<SessionAttendanceStudentRowDto> students
) {
    /**
     * One student row: attendance record + list of absence notices for this student and session.
     */
    public record SessionAttendanceStudentRowDto(
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
