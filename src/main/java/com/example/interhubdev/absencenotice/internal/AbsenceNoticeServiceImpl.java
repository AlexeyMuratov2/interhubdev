package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.absencenotice.*;
import com.example.interhubdev.absencenotice.internal.integration.AbsenceNoticeAttachedEventPayload;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import com.example.interhubdev.schedule.LessonDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link AbsenceNoticeApi}.
 */
@Service
@RequiredArgsConstructor
class AbsenceNoticeServiceImpl implements AbsenceNoticeApi {

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeLessonRepository lessonRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final SessionGateway sessionGateway;
    private final AbsenceNoticeAccessPolicy accessPolicy;
    private final AttendanceRecordAttachmentPort recordAttachmentPort;
    private final OutboxIntegrationEventPublisher publisher;
    private final CreateAbsenceNoticeUseCase createUseCase;
    private final UpdateAbsenceNoticeUseCase updateUseCase;
    private final CancelAbsenceNoticeUseCase cancelUseCase;
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
            java.time.LocalDateTime from,
            java.time.LocalDateTime to,
            UUID cursor,
            Integer limit
    ) {
        return getMyNoticesUseCase.execute(studentId, from, to, cursor, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<StudentNoticeSummaryDto>> getSessionNotices(UUID sessionId, boolean includeCanceled) {
        List<AbsenceNoticeLesson> lessonLinks = lessonRepository.findByLessonSessionId(sessionId);
        if (lessonLinks.isEmpty()) {
            return Map.of();
        }
        List<UUID> noticeIds = lessonLinks.stream().map(AbsenceNoticeLesson::getNoticeId).distinct().toList();
        List<AbsenceNoticeStatus> statuses = includeCanceled
                ? List.of(AbsenceNoticeStatus.SUBMITTED, AbsenceNoticeStatus.CANCELED)
                : List.of(AbsenceNoticeStatus.SUBMITTED);
        List<AbsenceNotice> notices = noticeRepository.findByIdInAndStatusInOrderBySubmittedAtDescIdDesc(noticeIds, statuses);
        if (notices.isEmpty()) {
            return Map.of();
        }

        Set<UUID> noticeIdSet = notices.stream().map(AbsenceNotice::getId).collect(Collectors.toSet());
        Map<UUID, List<String>> fileIdsByNoticeId = new HashMap<>();
        List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdIn(new ArrayList<>(noticeIdSet));
        for (AbsenceNoticeAttachment a : attachments) {
            fileIdsByNoticeId.computeIfAbsent(a.getNoticeId(), k -> new ArrayList<>()).add(a.getFileId());
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
        List<AbsenceNoticeLesson> lessonLinks = lessonRepository.findByLessonSessionId(sessionId);
        if (lessonLinks.isEmpty()) {
            return List.of();
        }
        List<UUID> noticeIds = lessonLinks.stream().map(AbsenceNoticeLesson::getNoticeId).distinct().toList();
        List<AbsenceNoticeStatus> statuses = includeCanceled
                ? List.of(AbsenceNoticeStatus.SUBMITTED, AbsenceNoticeStatus.CANCELED)
                : List.of(AbsenceNoticeStatus.SUBMITTED);
        List<AbsenceNotice> notices = noticeRepository.findByIdInAndStatusInOrderBySubmittedAtDescIdDesc(noticeIds, statuses);
        List<AbsenceNoticeDto> result = new ArrayList<>();
        for (AbsenceNotice n : notices) {
            List<AbsenceNoticeLesson> lessons = lessonRepository.findByNoticeIdOrderByLessonSessionId(n.getId());
            List<UUID> lessonIds = lessons.stream().map(AbsenceNoticeLesson::getLessonSessionId).toList();
            List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(n.getId());
            result.add(AbsenceNoticeMappers.toDto(n, lessonIds, attachments));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<StudentNoticeSummaryDto>> getNoticesByStudentAndLessons(UUID studentId, List<UUID> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return Map.of();
        }
        List<AbsenceNoticeLesson> lessonLinks = lessonRepository.findByLessonSessionIdIn(lessonIds);
        if (lessonLinks.isEmpty()) {
            return Map.of();
        }
        List<UUID> noticeIds = lessonLinks.stream().map(AbsenceNoticeLesson::getNoticeId).distinct().toList();
        List<AbsenceNotice> notices = noticeRepository.findByIdInOrderBySubmittedAtDescIdDesc(noticeIds);
        notices = notices.stream().filter(n -> n.getStudentId().equals(studentId)).toList();
        if (notices.isEmpty()) {
            return Map.of();
        }

        Set<UUID> noticeIdSet = notices.stream().map(AbsenceNotice::getId).collect(Collectors.toSet());
        Map<UUID, List<String>> fileIdsByNoticeId = new HashMap<>();
        List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdIn(new ArrayList<>(noticeIdSet));
        for (AbsenceNoticeAttachment a : attachments) {
            fileIdsByNoticeId.computeIfAbsent(a.getNoticeId(), k -> new ArrayList<>()).add(a.getFileId());
        }

        Map<UUID, Set<UUID>> lessonToNoticeIds = new HashMap<>();
        for (AbsenceNoticeLesson anl : lessonLinks) {
            if (!noticeIdSet.contains(anl.getNoticeId())) {
                continue;
            }
            lessonToNoticeIds
                    .computeIfAbsent(anl.getLessonSessionId(), k -> new HashSet<>())
                    .add(anl.getNoticeId());
        }

        Map<UUID, List<StudentNoticeSummaryDto>> byLesson = new HashMap<>();
        for (UUID lessonId : lessonIds) {
            Set<UUID> nIds = lessonToNoticeIds.getOrDefault(lessonId, Set.of());
            List<StudentNoticeSummaryDto> list = notices.stream()
                    .filter(n -> nIds.contains(n.getId()))
                    .map(n -> new StudentNoticeSummaryDto(
                            n.getId(),
                            n.getType(),
                            n.getStatus(),
                            Optional.ofNullable(n.getReasonText()).filter(s -> !s.isBlank()),
                            n.getSubmittedAt(),
                            fileIdsByNoticeId.getOrDefault(n.getId(), List.of())
                    ))
                    .toList();
            byLesson.put(lessonId, list);
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

        UUID recordLessonId = recordAttachmentPort.getLessonSessionIdByRecordId(recordId)
                .orElseThrow(() -> AbsenceNoticeErrors.recordNotFound(recordId));
        List<AbsenceNoticeLesson> noticeLessons = lessonRepository.findByNoticeIdOrderByLessonSessionId(noticeId);
        boolean noticeCoversRecordLesson = noticeLessons.stream()
                .anyMatch(anl -> anl.getLessonSessionId().equals(recordLessonId));
        if (!noticeCoversRecordLesson) {
            throw AbsenceNoticeErrors.noticeDoesNotMatchRecord(noticeId, recordId, "Notice does not cover this lesson");
        }

        LessonDto lesson = sessionGateway.getSessionById(recordLessonId)
                .orElseThrow(() -> AbsenceNoticeErrors.sessionNotFound(recordLessonId));
        accessPolicy.ensureCanManageSession(requesterId, lesson);

        recordAttachmentPort.attachNotice(recordId, noticeId, requesterId);

        Instant occurredAt = Instant.now();
        AbsenceNoticeAttachedEventPayload payload = new AbsenceNoticeAttachedEventPayload(
                recordId,
                noticeId,
                recordLessonId,
                notice.getStudentId(),
                requesterId,
                occurredAt
        );
        publisher.publish(OutboxEventDraft.builder()
                .eventType(AbsenceNoticeEventTypes.ABSENCE_NOTICE_ATTACHED)
                .payload(payload)
                .occurredAt(occurredAt)
                .build());

        List<UUID> lessonIds = noticeLessons.stream().map(AbsenceNoticeLesson::getLessonSessionId).toList();
        List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);
        return AbsenceNoticeMappers.toDto(notice, lessonIds, attachments);
    }

    @Override
    @Transactional
    public AbsenceNoticeDto detachFromRecordByRecordId(UUID recordId, UUID requesterId) {
        UUID noticeId = recordAttachmentPort.getNoticeIdByRecordId(recordId).orElse(null);
        if (noticeId == null) {
            return null;
        }
        AbsenceNotice notice = noticeRepository.findById(noticeId).orElse(null);
        if (notice == null) {
            recordAttachmentPort.detachNotice(recordId, requesterId);
            return null;
        }
        UUID recordLessonId = recordAttachmentPort.getLessonSessionIdByRecordId(recordId).orElse(null);
        if (recordLessonId != null) {
            LessonDto lesson = sessionGateway.getSessionById(recordLessonId).orElse(null);
            if (lesson != null) {
                accessPolicy.ensureCanManageSession(requesterId, lesson);
            }
        }
        recordAttachmentPort.detachNotice(recordId, requesterId);
        List<AbsenceNoticeLesson> lessons = lessonRepository.findByNoticeIdOrderByLessonSessionId(noticeId);
        List<UUID> lessonIds = lessons.stream().map(AbsenceNoticeLesson::getLessonSessionId).toList();
        List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);
        return AbsenceNoticeMappers.toDto(notice, lessonIds, attachments);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findLastSubmittedNoticeIdForSessionAndStudent(UUID sessionId, UUID studentId) {
        List<AbsenceNoticeLesson> lessonLinks = lessonRepository.findByLessonSessionId(sessionId);
        if (lessonLinks.isEmpty()) {
            return Optional.empty();
        }
        List<UUID> noticeIds = lessonLinks.stream().map(AbsenceNoticeLesson::getNoticeId).distinct().toList();
        List<AbsenceNotice> notices = noticeRepository.findByIdInAndStudentIdAndStatusOrderBySubmittedAtDesc(
                noticeIds, studentId, AbsenceNoticeStatus.SUBMITTED, PageRequest.of(0, 1));
        return notices.isEmpty() ? Optional.empty() : Optional.of(notices.get(0).getId());
    }
}
