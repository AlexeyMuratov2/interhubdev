package com.example.interhubdev.attendancerecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * DTO for attendance records of a lesson session (records only, no absence notices list).
 */
public record SessionRecordsDto(
        UUID sessionId,
        Map<AttendanceStatus, Integer> counts,
        Integer unmarkedCount,
        List<SessionRecordRowDto> students
) {
    /**
     * One student row: attendance record data only.
     */
    public record SessionRecordRowDto(
            UUID studentId,
            AttendanceStatus status,
            Integer minutesLate,
            String teacherComment,
            LocalDateTime markedAt,
            UUID markedBy,
            Optional<UUID> absenceNoticeId
    ) {
    }
}
