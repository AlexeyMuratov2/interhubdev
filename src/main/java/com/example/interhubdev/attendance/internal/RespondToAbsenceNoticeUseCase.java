package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeDto;
import com.example.interhubdev.attendance.AbsenceNoticeStatus;
import com.example.interhubdev.attendance.internal.integration.AbsenceNoticeTeacherRespondedEventPayload;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import com.example.interhubdev.schedule.LessonDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Use case for teacher responding (approve or reject) to an absence notice.
 * Publishes integration event when teacher responds.
 */
@Service
@RequiredArgsConstructor
@Transactional
class RespondToAbsenceNoticeUseCase {

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final SessionGateway sessionGateway;
    private final AttendanceAccessPolicy accessPolicy;
    private final OutboxIntegrationEventPublisher publisher;

    /**
     * Respond to an absence notice (approve or reject) with optional comment.
     *
     * @param noticeId  notice ID
     * @param approved   true to approve, false to reject
     * @param comment    optional teacher comment
     * @param teacherId  user ID of the teacher responding
     * @return updated notice DTO
     * @throws com.example.interhubdev.error.AppException if notice not found, teacher cannot respond, or notice not SUBMITTED
     */
    AbsenceNoticeDto execute(UUID noticeId, boolean approved, String comment, UUID teacherId) {
        AbsenceNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> AttendanceErrors.noticeNotFound(noticeId));

        if (notice.getStatus() != AbsenceNoticeStatus.SUBMITTED) {
            throw AttendanceErrors.noticeAlreadyResponded(noticeId);
        }

        LessonDto session = sessionGateway.getSessionById(notice.getLessonSessionId())
                .orElseThrow(() -> AttendanceErrors.sessionNotFound(notice.getLessonSessionId()));

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
                .eventType(AttendanceEventTypes.ABSENCE_NOTICE_TEACHER_RESPONDED)
                .payload(payload)
                .occurredAt(occurredAt)
                .build());

        return AbsenceNoticeMappers.toDto(saved, attachments);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : Instant.now();
    }
}
