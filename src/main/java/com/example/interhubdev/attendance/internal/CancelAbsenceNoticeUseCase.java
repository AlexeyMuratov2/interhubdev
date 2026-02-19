package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeDto;
import com.example.interhubdev.attendance.AbsenceNoticeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Use case for canceling absence notices.
 * Students can cancel their active (SUBMITTED) notices.
 */
@Service
@RequiredArgsConstructor
@Transactional
class CancelAbsenceNoticeUseCase {

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;

    /**
     * Cancel an absence notice.
     *
     * @param noticeId  notice ID
     * @param studentId student profile ID (for ownership validation)
     * @return canceled notice DTO
     * @throws com.example.interhubdev.error.AppException if notice not found, not owned by student, or not cancelable
     */
    AbsenceNoticeDto execute(UUID noticeId, UUID studentId) {
        AbsenceNotice notice = noticeRepository.findByIdAndStudentId(noticeId, studentId)
                .orElseThrow(() -> AttendanceErrors.noticeNotFound(noticeId));

        // Validate ownership
        if (!notice.getStudentId().equals(studentId)) {
            throw AttendanceErrors.noticeNotOwned(noticeId);
        }

        // Validate status
        if (notice.getStatus() != AbsenceNoticeStatus.SUBMITTED) {
            throw AttendanceErrors.noticeNotCancelable(noticeId,
                    "Only SUBMITTED notices can be canceled. Current status: " + notice.getStatus());
        }

        // Cancel notice
        notice.setStatus(AbsenceNoticeStatus.CANCELED);
        notice.setCanceledAt(LocalDateTime.now());
        AbsenceNotice saved = noticeRepository.save(notice);

        // Load attachments
        List<AbsenceNoticeAttachment> attachments = attachmentRepository
                .findByNoticeIdOrderByCreatedAtAsc(noticeId);

        // TODO: publish IntegrationEvent AbsenceNoticeCanceled {
        //   noticeId, sessionId, studentId, canceledAt
        // }

        return AbsenceNoticeMappers.toDto(saved, attachments);
    }
}
