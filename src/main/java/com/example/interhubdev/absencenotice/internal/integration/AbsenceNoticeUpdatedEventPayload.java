package com.example.interhubdev.absencenotice.internal.integration;

import com.example.interhubdev.absencenotice.AbsenceNoticeType;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload for absence notice updated event.
 */
public record AbsenceNoticeUpdatedEventPayload(
        UUID noticeId,
        UUID sessionId,
        UUID studentId,
        AbsenceNoticeType type,
        Instant updatedAt
) {
}
