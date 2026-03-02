package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.absencenotice.AbsenceNoticeDto;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Entity to DTO mapping for absence notices.
 */
final class AbsenceNoticeMappers {

    private AbsenceNoticeMappers() {
    }

    static AbsenceNoticeDto toDto(AbsenceNotice notice, List<UUID> lessonSessionIds, List<AbsenceNoticeAttachment> attachments) {
        List<String> fileIds = attachments.stream()
                .map(AbsenceNoticeAttachment::getFileId)
                .collect(Collectors.toList());

        return new AbsenceNoticeDto(
                notice.getId(),
                lessonSessionIds != null ? lessonSessionIds : List.of(),
                notice.getStudentId(),
                notice.getType(),
                Optional.ofNullable(notice.getReasonText()).filter(s -> !s.isBlank()),
                notice.getStatus(),
                notice.getSubmittedAt(),
                notice.getUpdatedAt(),
                Optional.ofNullable(notice.getCanceledAt()),
                fileIds
        );
    }
}
