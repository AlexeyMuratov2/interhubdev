package com.example.interhubdev.attendance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal DTO for an absence notice in batch/student context (for UI display).
 */
public record StudentNoticeSummaryDto(
        UUID id,
        AbsenceNoticeType type,
        AbsenceNoticeStatus status,
        Optional<String> reasonText,
        LocalDateTime submittedAt,
        List<String> fileIds
) {
}
