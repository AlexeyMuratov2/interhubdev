package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeDto;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Entity to DTO mapping for absence notices. No instantiation.
 */
final class AbsenceNoticeMappers {

    private AbsenceNoticeMappers() {
    }

    /**
     * Map AbsenceNotice entity to DTO with attachments.
     *
     * @param notice      notice entity
     * @param attachments list of attachment entities
     * @return DTO
     */
    static AbsenceNoticeDto toDto(AbsenceNotice notice, List<AbsenceNoticeAttachment> attachments) {
        List<String> fileIds = attachments.stream()
                .map(AbsenceNoticeAttachment::getFileId)
                .collect(Collectors.toList());

        return new AbsenceNoticeDto(
                notice.getId(),
                notice.getLessonSessionId(),
                notice.getStudentId(),
                notice.getType(),
                Optional.ofNullable(notice.getReasonText()).filter(s -> !s.isBlank()),
                notice.getStatus(),
                notice.getSubmittedAt(),
                notice.getUpdatedAt(),
                Optional.ofNullable(notice.getCanceledAt()),
                Optional.ofNullable(notice.getAttachedRecordId()),
                fileIds,
                Optional.ofNullable(notice.getTeacherComment()).filter(s -> !s.isBlank()),
                Optional.ofNullable(notice.getRespondedAt()),
                Optional.ofNullable(notice.getRespondedBy())
        );
    }
}
