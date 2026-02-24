package com.example.interhubdev.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for group attendance summary (per-student counts and attendance percent).
 * Attendance percent is the single source of truth: computed only from lessons
 * where at least one attendance mark was recorded (unmarked lessons are excluded).
 */
public record GroupAttendanceSummaryDto(
        UUID groupId,
        LocalDate from,
        LocalDate to,
        List<GroupAttendanceRowDto> rows
) {
    /**
     * Per-student attendance summary row.
     * {@code attendancePercent}: (PRESENT + LATE) / sessionsWithAtLeastOneMark * 100;
     * null if there are no lessons with any attendance mark in the range.
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
