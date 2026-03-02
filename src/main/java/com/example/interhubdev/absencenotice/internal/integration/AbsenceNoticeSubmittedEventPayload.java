package com.example.interhubdev.absencenotice.internal.integration;

import com.example.interhubdev.absencenotice.AbsenceNoticeType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload for absence notice submitted event. One notice can cover multiple lessons.
 */
public record AbsenceNoticeSubmittedEventPayload(
        UUID noticeId,
        List<UUID> sessionIds,
        UUID studentId,
        AbsenceNoticeType type,
        Instant submittedAt
) {
}
