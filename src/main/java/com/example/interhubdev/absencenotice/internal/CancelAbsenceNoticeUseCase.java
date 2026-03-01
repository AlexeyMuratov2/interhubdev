package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.absencenotice.AbsenceNoticeStatus;
import com.example.interhubdev.absencenotice.internal.integration.AbsenceNoticeCanceledEventPayload;
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

@Service
@RequiredArgsConstructor
@Transactional
class CancelAbsenceNoticeUseCase {

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final OutboxIntegrationEventPublisher publisher;

    AbsenceNoticeDto execute(UUID noticeId, UUID studentId) {
        AbsenceNotice notice = noticeRepository.findByIdAndStudentId(noticeId, studentId)
                .orElseThrow(() -> AbsenceNoticeErrors.noticeNotFound(noticeId));

        if (!notice.getStudentId().equals(studentId)) {
            throw AbsenceNoticeErrors.noticeNotOwned(noticeId);
        }

        if (notice.getStatus() == AbsenceNoticeStatus.APPROVED || notice.getStatus() == AbsenceNoticeStatus.REJECTED) {
            throw AbsenceNoticeErrors.noticeCannotBeCanceledAfterResponse(noticeId);
        }
        if (notice.getStatus() != AbsenceNoticeStatus.SUBMITTED) {
            throw AbsenceNoticeErrors.noticeNotCancelable(noticeId,
                    "Only SUBMITTED notices can be canceled. Current status: " + notice.getStatus());
        }

        notice.setStatus(AbsenceNoticeStatus.CANCELED);
        notice.setCanceledAt(LocalDateTime.now());
        AbsenceNotice saved = noticeRepository.save(notice);

        List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);

        Instant occurredAt = Instant.now();
        AbsenceNoticeCanceledEventPayload payload = new AbsenceNoticeCanceledEventPayload(
                saved.getId(),
                saved.getLessonSessionId(),
                saved.getStudentId(),
                toInstant(saved.getCanceledAt())
        );
        publisher.publish(OutboxEventDraft.builder()
                .eventType(AbsenceNoticeEventTypes.ABSENCE_NOTICE_CANCELED)
                .payload(payload)
                .occurredAt(occurredAt)
                .build());

        return AbsenceNoticeMappers.toDto(saved, attachments);
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : Instant.now();
    }
}
