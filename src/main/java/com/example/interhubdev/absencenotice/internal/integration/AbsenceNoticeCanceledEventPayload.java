package com.example.interhubdev.absencenotice.internal.integration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload for absence notice canceled event. One notice can cover multiple lessons.
 */
public record AbsenceNoticeCanceledEventPayload(
        UUID noticeId,
        List<UUID> sessionIds,
        UUID studentId,
        Instant canceledAt
) {
}
