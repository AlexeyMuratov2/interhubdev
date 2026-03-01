package com.example.interhubdev.absencenotice;

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
        List<String> fileIds,
        Optional<String> teacherComment,
        Optional<LocalDateTime> respondedAt,
        Optional<UUID> respondedBy
) {
}
