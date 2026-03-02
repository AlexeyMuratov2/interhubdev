package com.example.interhubdev.absencenotice.internal.integration;

import com.example.interhubdev.absencenotice.AbsenceNoticeType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload for absence notice updated event. One notice can cover multiple lessons.
 * When all lessons belong to a single offering (one subject), {@code singleOfferingId} is set
 * so notification content can include subject name; otherwise it is null.
 */
public record AbsenceNoticeUpdatedEventPayload(
        UUID noticeId,
        List<UUID> sessionIds,
        UUID studentId,
        AbsenceNoticeType type,
        Instant updatedAt,
        Instant periodStart,
        Instant periodEnd,
        UUID singleOfferingId
) {
}
