package com.example.interhubdev.attendance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Item for bulk attendance marking.
 */
public record MarkAttendanceItem(
        @NotNull(message = "studentId is required")
        UUID studentId,
        @NotNull(message = "status is required")
        AttendanceStatus status,
        Integer minutesLate,
        @Size(max = 2000, message = "teacherComment must not exceed 2000 characters")
        String teacherComment
) {
}
