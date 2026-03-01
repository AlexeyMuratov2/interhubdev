package com.example.interhubdev.attendancerecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for group attendance summary (per-student counts and attendance percent).
 */
public record GroupAttendanceSummaryDto(
        UUID groupId,
        LocalDate from,
        LocalDate to,
        List<GroupAttendanceRowDto> rows
) {
    /**
     * Per-student attendance summary row.
     */
    public record GroupAttendanceRowDto(
            UUID studentId,
            Map<AttendanceStatus, Integer> summary,
            Integer totalMarked,
            Integer unmarkedCount,
            Double attendancePercent
    ) {
    }
}
