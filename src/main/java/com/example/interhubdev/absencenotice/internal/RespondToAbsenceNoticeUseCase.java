package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.absencenotice.AbsenceNoticeStatus;
import com.example.interhubdev.absencenotice.internal.integration.AbsenceNoticeTeacherRespondedEventPayload;
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
class RespondToAbsenceNoticeUseCase {

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final SessionGateway sessionGateway;
    private final AbsenceNoticeAccessPolicy accessPolicy;
    private final OutboxIntegrationEventPublisher publisher;

    AbsenceNoticeDto execute(UUID noticeId, boolean approved, String comment, UUID teacherId) {
        AbsenceNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> AbsenceNoticeErrors.noticeNotFound(noticeId));

        if (notice.getStatus() != AbsenceNoticeStatus.SUBMITTED) {
            throw AbsenceNoticeErrors.noticeAlreadyResponded(noticeId);
        }

        var session = sessionGateway.getSessionById(notice.getLessonSessionId())
                .orElseThrow(() -> AbsenceNoticeErrors.sessionNotFound(notice.getLessonSessionId()));

        accessPolicy.ensureCanManageSession(teacherId, session);

        LocalDateTime now = LocalDateTime.now();
        notice.setStatus(approved ? AbsenceNoticeStatus.APPROVED : AbsenceNoticeStatus.REJECTED);
        notice.setTeacherComment(comment != null && !comment.isBlank() ? comment : null);
        notice.setRespondedAt(now);
        notice.setRespondedBy(teacherId);

        AbsenceNotice saved = noticeRepository.save(notice);

        List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);

        Instant occurredAt = Instant.now();
        AbsenceNoticeTeacherRespondedEventPayload payload = new AbsenceNoticeTeacherRespondedEventPayload(
                saved.getId(),
                saved.getLessonSessionId(),
                saved.getStudentId(),
                approved,
                saved.getTeacherComment(),
                toInstant(saved.getRespondedAt()),
                saved.getRespondedBy()
        );
        publisher.publish(OutboxEventDraft.builder()
                .eventType(AbsenceNoticeEventTypes.ABSENCE_NOTICE_TEACHER_RESPONDED)
                .payload(payload)
                .occurredAt(occurredAt)
                .build());

        return AbsenceNoticeMappers.toDto(saved, attachments);
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : Instant.now();
    }
}
