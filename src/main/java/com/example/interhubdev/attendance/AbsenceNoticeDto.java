package com.example.interhubdev.attendance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DTO for absence notice submitted by a student.
 */
public record AbsenceNoticeDto(
        UUID id,
        UUID lessonSessionId,
        UUID studentId,
        AbsenceNoticeType type,
        Optional<String> reasonText,
        AbsenceNoticeStatus status,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt,
        Optional<LocalDateTime> canceledAt,
        Optional<UUID> attachedRecordId,
        List<String> fileIds // File IDs from Document module
) {
}
