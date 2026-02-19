package com.example.interhubdev.attendance.internal.integration;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload for absence notice attached to attendance record event.
 */
public record AbsenceNoticeAttachedEventPayload(
        UUID recordId,
        UUID noticeId,
        UUID sessionId,
        UUID studentId,
        UUID attachedBy,
        Instant attachedAt
) {
}
