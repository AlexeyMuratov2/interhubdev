package com.example.interhubdev.attendance.internal.integration;

import com.example.interhubdev.attendancerecord.AttendanceStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload for attendance marked event (published by facade).
 */
public record AttendanceMarkedEventPayload(
        UUID recordId,
        UUID sessionId,
        UUID studentId,
        AttendanceStatus status,
        UUID markedBy,
        Instant markedAt
) {
}
