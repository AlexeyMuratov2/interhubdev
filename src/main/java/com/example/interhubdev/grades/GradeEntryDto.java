package com.example.interhubdev.grades;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * DTO for a single grade ledger entry (allocation or correction).
 */
public record GradeEntryDto(
    UUID id,
    UUID studentId,
    UUID offeringId,
    java.math.BigDecimal points,
    GradeTypeCode typeCode,
    Optional<String> typeLabel,
    Optional<String> description,
    Optional<UUID> lessonSessionId,
    Optional<UUID> homeworkSubmissionId,
    UUID gradedBy,
    LocalDateTime gradedAt,
    String status
) {
}
