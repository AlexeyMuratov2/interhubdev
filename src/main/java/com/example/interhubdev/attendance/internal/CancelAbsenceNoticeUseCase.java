package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeDto;
import com.example.interhubdev.attendance.AbsenceNoticeStatus;
import com.example.interhubdev.attendance.internal.integration.AbsenceNoticeCanceledEventPayload;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final OutboxIntegrationEventPublisher publisher;

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

        // Validate status: only SUBMITTED can be canceled; cannot cancel after teacher responded
        if (notice.getStatus() == AbsenceNoticeStatus.APPROVED || notice.getStatus() == AbsenceNoticeStatus.REJECTED) {
            throw AttendanceErrors.noticeCannotBeCanceledAfterResponse(noticeId);
        }
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

        // Publish integration event
        Instant occurredAt = Instant.now();
        AbsenceNoticeCanceledEventPayload payload = new AbsenceNoticeCanceledEventPayload(
                saved.getId(),
                saved.getLessonSessionId(),
                saved.getStudentId(),
                toInstant(saved.getCanceledAt())
        );
        publisher.publish(OutboxEventDraft.builder()
                .eventType(AttendanceEventTypes.ABSENCE_NOTICE_CANCELED)
                .payload(payload)
                .occurredAt(occurredAt)
                .build());

        return AbsenceNoticeMappers.toDto(saved, attachments);
    }

    /**
     * Convert LocalDateTime to Instant using UTC offset.
     */
    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : Instant.now();
    }
}
