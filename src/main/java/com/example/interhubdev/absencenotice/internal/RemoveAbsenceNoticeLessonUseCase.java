package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.absencenotice.AbsenceNoticeStatus;
import com.example.interhubdev.absencenotice.internal.integration.AbsenceNoticeCanceledEventPayload;
import com.example.interhubdev.absencenotice.internal.integration.AbsenceNoticeUpdatedEventPayload;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
class RemoveAbsenceNoticeLessonUseCase {

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeLessonRepository lessonRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final SessionGateway sessionGateway;
    private final OutboxIntegrationEventPublisher publisher;

    AbsenceNoticeDto execute(UUID noticeId, UUID lessonSessionId, UUID studentId) {
        AbsenceNotice notice = noticeRepository.findByIdAndStudentId(noticeId, studentId)
                .orElseThrow(() -> AbsenceNoticeErrors.noticeNotFound(noticeId));

        if (notice.getStatus() != AbsenceNoticeStatus.SUBMITTED) {
            throw AbsenceNoticeErrors.noticeNotCancelable(
                    noticeId,
                    "Only SUBMITTED notices can be changed. Current status: " + notice.getStatus()
            );
        }

        if (!lessonRepository.existsByNoticeIdAndLessonSessionId(noticeId, lessonSessionId)) {
            throw AbsenceNoticeErrors.noticeDoesNotCoverSession(noticeId, lessonSessionId);
        }

        lessonRepository.deleteByNoticeIdAndLessonSessionId(noticeId, lessonSessionId);
        List<AbsenceNoticeLesson> remainingLessons = lessonRepository.findByNoticeIdOrderByLessonSessionId(noticeId);
        List<UUID> remainingSessionIds = remainingLessons.stream()
                .map(AbsenceNoticeLesson::getLessonSessionId)
                .collect(Collectors.toList());

        if (remainingSessionIds.isEmpty()) {
            notice.setStatus(AbsenceNoticeStatus.CANCELED);
            notice.setCanceledAt(LocalDateTime.now());
            AbsenceNotice saved = noticeRepository.save(notice);

            Instant occurredAt = Instant.now();
            AbsenceNoticeCanceledEventPayload canceledPayload = new AbsenceNoticeCanceledEventPayload(
                    saved.getId(),
                    List.of(),
                    saved.getStudentId(),
                    toInstant(saved.getCanceledAt())
            );
            publisher.publish(OutboxEventDraft.builder()
                    .eventType(AbsenceNoticeEventTypes.ABSENCE_NOTICE_CANCELED)
                    .payload(canceledPayload)
                    .occurredAt(occurredAt)
                    .build());

            List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);
            return AbsenceNoticeMappers.toDto(saved, List.of(), attachments);
        }

        AbsenceNotice saved = noticeRepository.save(notice);
        List<LessonDto> lessons = sessionGateway.getSessionsByIds(remainingSessionIds);
        Instant periodStart = computePeriodStart(lessons);
        Instant periodEnd = computePeriodEnd(lessons);
        UUID singleOfferingId = singleOfferingIdOrNull(lessons);
        Instant occurredAt = Instant.now();
        AbsenceNoticeUpdatedEventPayload updatedPayload = new AbsenceNoticeUpdatedEventPayload(
                saved.getId(),
                remainingSessionIds,
                saved.getStudentId(),
                saved.getType(),
                toInstant(saved.getUpdatedAt()),
                periodStart,
                periodEnd,
                singleOfferingId
        );
        publisher.publish(OutboxEventDraft.builder()
                .eventType(AbsenceNoticeEventTypes.ABSENCE_NOTICE_UPDATED)
                .payload(updatedPayload)
                .occurredAt(occurredAt)
                .build());

        List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);
        return AbsenceNoticeMappers.toDto(saved, remainingSessionIds, attachments);
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : Instant.now();
    }

    private static Instant computePeriodStart(List<LessonDto> lessons) {
        return lessons.stream()
                .flatMap(l -> Stream.of(LocalDateTime.of(l.date(), l.startTime())))
                .min(LocalDateTime::compareTo)
                .map(ldt -> ldt.toInstant(ZoneOffset.UTC))
                .orElse(Instant.now());
    }

    private static Instant computePeriodEnd(List<LessonDto> lessons) {
        return lessons.stream()
                .flatMap(l -> Stream.of(LocalDateTime.of(l.date(), l.endTime())))
                .max(LocalDateTime::compareTo)
                .map(ldt -> ldt.toInstant(ZoneOffset.UTC))
                .orElse(Instant.now());
    }

    /** When all lessons belong to one offering (one subject), returns that offering id; otherwise null. */
    private static UUID singleOfferingIdOrNull(List<LessonDto> lessons) {
        if (lessons == null || lessons.isEmpty()) return null;
        Set<UUID> offeringIds = lessons.stream()
                .map(LessonDto::offeringId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return offeringIds.size() == 1 ? offeringIds.iterator().next() : null;
    }
}
