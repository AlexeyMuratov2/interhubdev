package com.example.interhubdev.attendance;

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
        LocalDateTime updatedAt
) {
}
