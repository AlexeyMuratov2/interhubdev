package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.absencenotice.*;
import com.example.interhubdev.absencenotice.internal.integration.AbsenceNoticeAttachedEventPayload;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import com.example.interhubdev.schedule.LessonDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AbsenceNoticeApi}.
 */
@Service
@RequiredArgsConstructor
class AbsenceNoticeServiceImpl implements AbsenceNoticeApi {

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final SessionGateway sessionGateway;
    private final AbsenceNoticeAccessPolicy accessPolicy;
    private final AttendanceRecordAttachmentPort recordAttachmentPort;
    private final OutboxIntegrationEventPublisher publisher;
    private final CreateAbsenceNoticeUseCase createUseCase;
    private final UpdateAbsenceNoticeUseCase updateUseCase;
    private final CancelAbsenceNoticeUseCase cancelUseCase;
    private final RespondToAbsenceNoticeUseCase respondUseCase;
    private final GetTeacherAbsenceNoticesUseCase getTeacherNoticesUseCase;
    private final GetMyAbsenceNoticesUseCase getMyNoticesUseCase;

    @Override
    @Transactional
    public AbsenceNoticeDto createAbsenceNotice(SubmitAbsenceNoticeRequest request, UUID studentId) {
        return createUseCase.execute(request, studentId);
    }

    @Override
    @Transactional
    public AbsenceNoticeDto updateAbsenceNotice(UUID noticeId, SubmitAbsenceNoticeRequest request, UUID studentId) {
        return updateUseCase.execute(noticeId, request, studentId);
    }

    @Override
    @Transactional
    public AbsenceNoticeDto cancelAbsenceNotice(UUID noticeId, UUID studentId) {
        return cancelUseCase.execute(noticeId, studentId);
    }

    @Override
    @Transactional
    public AbsenceNoticeDto respondToAbsenceNotice(UUID noticeId, boolean approved, String comment, UUID teacherId) {
        return respondUseCase.execute(noticeId, approved, comment, teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherAbsenceNoticePage getTeacherAbsenceNotices(
            UUID teacherId,
            List<AbsenceNoticeStatus> statuses,
            UUID cursor,
            Integer limit
    ) {
        return getTeacherNoticesUseCase.execute(teacherId, statuses, cursor, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAbsenceNoticePage getMyAbsenceNotices(
            UUID studentId,
            LocalDateTime from,
            LocalDateTime to,
            UUID cursor,
            Integer limit
    ) {
        return getMyNoticesUseCase.execute(studentId, from, to, cursor, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<StudentNoticeSummaryDto>> getSessionNotices(UUID sessionId, boolean includeCanceled) {
        List<AbsenceNotice> notices = includeCanceled
                ? noticeRepository.findByLessonSessionId(sessionId)
                : noticeRepository.findByLessonSessionIdAndStatus(sessionId, AbsenceNoticeStatus.SUBMITTED);

        Set<UUID> noticeIds = notices.stream().map(AbsenceNotice::getId).collect(Collectors.toSet());
        Map<UUID, List<String>> fileIdsByNoticeId = new HashMap<>();
        if (!noticeIds.isEmpty()) {
            List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdIn(new ArrayList<>(noticeIds));
            for (AbsenceNoticeAttachment a : attachments) {
                fileIdsByNoticeId.computeIfAbsent(a.getNoticeId(), k -> new ArrayList<>()).add(a.getFileId());
            }
        }

        Map<UUID, List<StudentNoticeSummaryDto>> byStudent = new HashMap<>();
        for (AbsenceNotice n : notices) {
            List<String> fileIds = fileIdsByNoticeId.getOrDefault(n.getId(), List.of());
            StudentNoticeSummaryDto dto = new StudentNoticeSummaryDto(
                    n.getId(),
                    n.getType(),
                    n.getStatus(),
                    Optional.ofNullable(n.getReasonText()).filter(s -> !s.isBlank()),
                    n.getSubmittedAt(),
                    fileIds
            );
            byStudent.computeIfAbsent(n.getStudentId(), k -> new ArrayList<>()).add(dto);
        }
        return byStudent;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceNoticeDto> getSessionNoticesAsList(UUID sessionId, boolean includeCanceled) {
        List<AbsenceNotice> notices = includeCanceled
                ? noticeRepository.findByLessonSessionIdOrderBySubmittedAtDesc(sessionId)
                : noticeRepository.findByLessonSessionIdAndStatus(sessionId, AbsenceNoticeStatus.SUBMITTED);
        List<AbsenceNoticeDto> result = new ArrayList<>();
        for (AbsenceNotice n : notices) {
            List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(n.getId());
            result.add(AbsenceNoticeMappers.toDto(n, attachments));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<StudentNoticeSummaryDto>> getNoticesByStudentAndLessons(UUID studentId, List<UUID> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return Map.of();
        }
        List<AbsenceNotice> notices = noticeRepository.findByStudentIdAndLessonSessionIdIn(studentId, lessonIds);
        Set<UUID> noticeIds = notices.stream().map(AbsenceNotice::getId).collect(Collectors.toSet());
        Map<UUID, List<String>> fileIdsByNoticeId = new HashMap<>();
        if (!noticeIds.isEmpty()) {
            List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdIn(new ArrayList<>(noticeIds));
            for (AbsenceNoticeAttachment a : attachments) {
                fileIdsByNoticeId.computeIfAbsent(a.getNoticeId(), k -> new ArrayList<>()).add(a.getFileId());
            }
        }

        Map<UUID, List<StudentNoticeSummaryDto>> byLesson = new HashMap<>();
        for (AbsenceNotice n : notices) {
            List<String> fileIds = fileIdsByNoticeId.getOrDefault(n.getId(), List.of());
            StudentNoticeSummaryDto dto = new StudentNoticeSummaryDto(
                    n.getId(),
                    n.getType(),
                    n.getStatus(),
                    Optional.ofNullable(n.getReasonText()).filter(s -> !s.isBlank()),
                    n.getSubmittedAt(),
                    fileIds
            );
            byLesson.computeIfAbsent(n.getLessonSessionId(), k -> new ArrayList<>()).add(dto);
        }
        return byLesson;
    }

    @Override
    @Transactional
    public AbsenceNoticeDto attachToRecord(UUID noticeId, UUID recordId, UUID requesterId) {
        AbsenceNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> AbsenceNoticeErrors.noticeNotFound(noticeId));

        if (notice.getStatus() == AbsenceNoticeStatus.CANCELED) {
            throw AbsenceNoticeErrors.noticeCanceled(noticeId);
        }

        LessonDto lesson = sessionGateway.getSessionById(notice.getLessonSessionId())
                .orElseThrow(() -> AbsenceNoticeErrors.sessionNotFound(notice.getLessonSessionId()));
        accessPolicy.ensureCanManageSession(requesterId, lesson);

        notice.setAttachedRecordId(recordId);
        AbsenceNotice saved = noticeRepository.save(notice);

        recordAttachmentPort.attachNotice(recordId, noticeId, requesterId);

        Instant occurredAt = Instant.now();
        AbsenceNoticeAttachedEventPayload payload = new AbsenceNoticeAttachedEventPayload(
                recordId,
                noticeId,
                notice.getLessonSessionId(),
                notice.getStudentId(),
                requesterId,
                occurredAt
        );
        publisher.publish(OutboxEventDraft.builder()
                .eventType(AbsenceNoticeEventTypes.ABSENCE_NOTICE_ATTACHED)
                .payload(payload)
                .occurredAt(occurredAt)
                .build());

        List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);
        return AbsenceNoticeMappers.toDto(saved, attachments);
    }

    @Override
    @Transactional
    public AbsenceNoticeDto detachFromRecord(UUID noticeId, UUID requesterId) {
        AbsenceNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> AbsenceNoticeErrors.noticeNotFound(noticeId));

        LessonDto lesson = sessionGateway.getSessionById(notice.getLessonSessionId())
                .orElseThrow(() -> AbsenceNoticeErrors.sessionNotFound(notice.getLessonSessionId()));
        accessPolicy.ensureCanManageSession(requesterId, lesson);

        UUID recordId = notice.getAttachedRecordId();
        notice.setAttachedRecordId(null);
        AbsenceNotice saved = noticeRepository.save(notice);

        if (recordId != null) {
            recordAttachmentPort.detachNotice(recordId, requesterId);
        }

        List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);
        return AbsenceNoticeMappers.toDto(saved, attachments);
    }

    @Override
    @Transactional
    public AbsenceNoticeDto detachFromRecordByRecordId(UUID recordId, UUID requesterId) {
        AbsenceNotice notice = noticeRepository.findByAttachedRecordId(recordId).orElse(null);
        if (notice == null) {
            return null;
        }
        return detachFromRecord(notice.getId(), requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findLastSubmittedNoticeIdForSessionAndStudent(UUID sessionId, UUID studentId) {
        return noticeRepository.findLastSubmittedBySessionAndStudent(
                sessionId, studentId, AbsenceNoticeStatus.SUBMITTED
        ).map(AbsenceNotice::getId);
    }
}
