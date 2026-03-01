package com.example.interhubdev.absencenotice.internal.integration;

import com.example.interhubdev.absencenotice.AbsenceNoticeType;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload for absence notice submitted event.
 */
public record AbsenceNoticeSubmittedEventPayload(
        UUID noticeId,
        UUID sessionId,
        UUID studentId,
        AbsenceNoticeType type,
        Instant submittedAt
) {
}
