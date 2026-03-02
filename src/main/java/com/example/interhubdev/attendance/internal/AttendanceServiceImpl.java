package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.absencenotice.AbsenceNoticeApi;
import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.absencenotice.SubmitAbsenceNoticeRequest;
import com.example.interhubdev.attendancerecord.AttendanceRecordApi;
import com.example.interhubdev.attendancerecord.AttendanceRecordDto;
import com.example.interhubdev.attendancerecord.AttendanceStatus;
import com.example.interhubdev.attendancerecord.MarkAttendanceItem;
import com.example.interhubdev.attendance.AttendanceApi;
import com.example.interhubdev.attendance.internal.integration.AttendanceMarkedEventPayload;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Facade implementation of {@link AttendanceApi}: delegates to attendancerecord and absencenotice, merges where needed.
 */
@Service
@RequiredArgsConstructor
class AttendanceServiceImpl implements AttendanceApi {

    private static final String ATTENDANCE_MARKED_EVENT = "attendance.record.marked";

    private final AttendanceRecordApi recordApi;
    private final AbsenceNoticeApi noticeApi;
    private final OutboxIntegrationEventPublisher publisher;

    @Override
    @Transactional
    public List<AttendanceRecordDto> markAttendanceBulk(UUID sessionId, List<MarkAttendanceItem> items, UUID markedBy) {
        List<MarkAttendanceItem> resolved = resolveNoticeIdsForBulk(sessionId, items);
        List<AttendanceRecordDto> saved = recordApi.markAttendanceBulk(sessionId, resolved, markedBy);

        Instant occurredAt = Instant.now();
        for (AttendanceRecordDto record : saved) {
            publishAttendanceMarked(record, markedBy, occurredAt);
            record.absenceNoticeId().ifPresent(noticeId ->
                    noticeApi.attachToRecord(noticeId, record.id(), markedBy)); // validates notice covers lesson and updates link
        }
        return saved;
    }

    @Override
    @Transactional
    public AttendanceRecordDto markAttendanceSingle(
            UUID sessionId,
            UUID studentId,
            AttendanceStatus status,
            Integer minutesLate,
            String teacherComment,
            UUID absenceNoticeId,
            Boolean autoAttachLastNotice,
            UUID markedBy
    ) {
        UUID resolvedNoticeId = absenceNoticeId;
        if (resolvedNoticeId == null && Boolean.TRUE.equals(autoAttachLastNotice)) {
            resolvedNoticeId = noticeApi.findLastSubmittedNoticeIdForSessionAndStudent(sessionId, studentId).orElse(null);
        }
        AttendanceRecordDto saved = recordApi.markAttendanceSingle(
                sessionId, studentId, status, minutesLate, teacherComment, resolvedNoticeId, markedBy);

        Instant occurredAt = Instant.now();
        publishAttendanceMarked(saved, markedBy, occurredAt);
        saved.absenceNoticeId().ifPresent(noticeId ->
                noticeApi.attachToRecord(noticeId, saved.id(), markedBy));
        return saved;
    }

    @Override
    @Transactional
    public AbsenceNoticeDto createAbsenceNotice(SubmitAbsenceNoticeRequest request, UUID studentId) {
        return noticeApi.createAbsenceNotice(request, studentId);
    }

    @Override
    @Transactional
    public AbsenceNoticeDto updateAbsenceNotice(UUID noticeId, SubmitAbsenceNoticeRequest request, UUID studentId) {
        return noticeApi.updateAbsenceNotice(noticeId, request, studentId);
    }

    @Override
    @Transactional
    public AbsenceNoticeDto cancelAbsenceNotice(UUID noticeId, UUID studentId) {
        AbsenceNoticeDto dto = noticeApi.cancelAbsenceNotice(noticeId, studentId);
        recordApi.detachNoticeByNoticeId(noticeId);
        return dto;
    }

    @Override
    @Transactional
    public AbsenceNoticeDto removeLessonFromAbsenceNotice(UUID noticeId, UUID lessonSessionId, UUID studentId) {
        AbsenceNoticeDto dto = noticeApi.removeLessonFromAbsenceNotice(noticeId, lessonSessionId, studentId);
        recordApi.detachNoticeByNoticeIdAndSessionId(noticeId, lessonSessionId);
        return dto;
    }

    @Override
    @Transactional
    public AttendanceRecordDto attachNoticeToRecord(UUID recordId, UUID noticeId, UUID requesterId) {
        noticeApi.attachToRecord(noticeId, recordId, requesterId);
        return recordApi.findRecordById(recordId)
                .orElseThrow(() -> new IllegalStateException("Record not found after attach: " + recordId));
    }

    @Override
    @Transactional
    public AttendanceRecordDto detachNoticeFromRecord(UUID recordId, UUID requesterId) {
        return recordApi.detachNotice(recordId, requesterId);
    }

    private List<MarkAttendanceItem> resolveNoticeIdsForBulk(UUID sessionId, List<MarkAttendanceItem> items) {
        List<MarkAttendanceItem> resolved = new ArrayList<>();
        for (MarkAttendanceItem item : items) {
            UUID noticeId = item.absenceNoticeId();
            if (noticeId == null && Boolean.TRUE.equals(item.autoAttachLastNotice())) {
                noticeId = noticeApi.findLastSubmittedNoticeIdForSessionAndStudent(sessionId, item.studentId()).orElse(null);
            }
            resolved.add(new MarkAttendanceItem(
                    item.studentId(),
                    item.status(),
                    item.minutesLate(),
                    item.teacherComment(),
                    noticeId,
                    null
            ));
        }
        return resolved;
    }

    private void publishAttendanceMarked(AttendanceRecordDto record, UUID markedBy, Instant occurredAt) {
        Instant markedAt = record.markedAt() != null
                ? record.markedAt().toInstant(ZoneOffset.UTC)
                : occurredAt;
        AttendanceMarkedEventPayload payload = new AttendanceMarkedEventPayload(
                record.id(),
                record.lessonSessionId(),
                record.studentId(),
                record.status(),
                markedBy,
                markedAt
        );
        publisher.publish(OutboxEventDraft.builder()
                .eventType(ATTENDANCE_MARKED_EVENT)
                .payload(payload)
                .occurredAt(occurredAt)
                .build());
    }
}
