package com.example.interhubdev.attendance.internal.integration;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload for absence notice teacher responded event.
 */
public record AbsenceNoticeTeacherRespondedEventPayload(
        UUID noticeId,
        UUID sessionId,
        UUID studentId,
        boolean approved,
        String teacherComment,
        Instant respondedAt,
        UUID respondedBy
) {
}
