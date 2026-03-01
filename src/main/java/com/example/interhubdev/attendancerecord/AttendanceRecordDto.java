package com.example.interhubdev.attendancerecord;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * DTO for a single attendance record.
 */
public record AttendanceRecordDto(
        UUID id,
        UUID lessonSessionId,
        UUID studentId,
        AttendanceStatus status,
        Optional<Integer> minutesLate,
        Optional<String> teacherComment,
        UUID markedBy,
        LocalDateTime markedAt,
        LocalDateTime updatedAt,
        /**
         * Optional link to absence notice attached to this record.
         * Single source of truth: stored in attendance_record.absence_notice_id.
         */
        Optional<UUID> absenceNoticeId
) {
}
