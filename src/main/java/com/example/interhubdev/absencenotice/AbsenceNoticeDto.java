package com.example.interhubdev.absencenotice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DTO for absence notice submitted by a student. Covers multiple lessons via lessonSessionIds.
 */
public record AbsenceNoticeDto(
        UUID id,
        List<UUID> lessonSessionIds,
        UUID studentId,
        AbsenceNoticeType type,
        Optional<String> reasonText,
        AbsenceNoticeStatus status,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt,
        Optional<LocalDateTime> canceledAt,
        List<String> fileIds
) {
}
