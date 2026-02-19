package com.example.interhubdev.attendance.internal.integration;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload for absence notice canceled event.
 */
public record AbsenceNoticeCanceledEventPayload(
        UUID noticeId,
        UUID sessionId,
        UUID studentId,
        Instant canceledAt
) {
}
