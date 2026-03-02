package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.absencenotice.AbsenceNoticeApi;
import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.absencenotice.AbsenceNoticeStatus;
import com.example.interhubdev.absencenotice.StudentAbsenceNoticePage;
import com.example.interhubdev.absencenotice.SubmitAbsenceNoticeRequest;
import com.example.interhubdev.absencenotice.TeacherAbsenceNoticePage;
import com.example.interhubdev.attendancerecord.AttendanceRecordApi;
import com.example.interhubdev.attendancerecord.AttendanceRecordDto;
import com.example.interhubdev.attendancerecord.AttendanceStatus;
import com.example.interhubdev.attendancerecord.GroupAttendanceSummaryDto;
import com.example.interhubdev.attendancerecord.MarkAttendanceItem;
import com.example.interhubdev.attendancerecord.SessionRecordsDto;
import com.example.interhubdev.attendancerecord.StudentAttendanceDto;
import com.example.interhubdev.attendancerecord.StudentAttendanceRecordsByLessonsDto;
import com.example.interhubdev.attendance.AttendanceApi;
import com.example.interhubdev.attendance.SessionAttendanceDto;
import com.example.interhubdev.attendance.StudentAttendanceByLessonsDto;
import com.example.interhubdev.attendance.StudentLessonAttendanceItemDto;
import com.example.interhubdev.attendance.internal.integration.AttendanceMarkedEventPayload;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    @Transactional(readOnly = true)
    public SessionAttendanceDto getSessionAttendance(UUID sessionId, UUID requesterId, boolean includeCanceled) {
        SessionRecordsDto records = recordApi.getSessionRecords(sessionId, requesterId);
        Map<UUID, List<com.example.interhubdev.absencenotice.StudentNoticeSummaryDto>> noticesByStudent =
                noticeApi.getSessionNotices(sessionId, includeCanceled);

        List<SessionAttendanceDto.SessionAttendanceStudentDto> students = new ArrayList<>();
        for (SessionRecordsDto.SessionRecordRowDto row : records.students()) {
            List<com.example.interhubdev.absencenotice.StudentNoticeSummaryDto> notices =
                    noticesByStudent.getOrDefault(row.studentId(), List.of());
            students.add(new SessionAttendanceDto.SessionAttendanceStudentDto(
                    row.studentId(),
                    row.status(),
                    row.minutesLate(),
                    row.teacherComment(),
                    row.markedAt(),
                    row.markedBy(),
                    row.absenceNoticeId(),
                    notices
            ));
        }
        return new SessionAttendanceDto(
                records.sessionId(),
                records.counts(),
                records.unmarkedCount(),
                students
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceNoticeDto> getSessionNotices(UUID sessionId, UUID requesterId, boolean includeCanceled) {
        recordApi.getSessionRecords(sessionId, requesterId);
        return noticeApi.getSessionNoticesAsList(sessionId, includeCanceled);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceDto getStudentAttendance(
            UUID studentId,
            LocalDateTime from,
            LocalDateTime to,
            UUID offeringId,
            UUID groupId,
            UUID requesterId
    ) {
        return recordApi.getStudentAttendance(studentId, from, to, offeringId, groupId, requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceByLessonsDto getStudentAttendanceByLessonIds(
            UUID studentId,
            List<UUID> lessonIds,
            UUID requesterId
    ) {
        StudentAttendanceRecordsByLessonsDto records = recordApi.getStudentAttendanceByLessonIds(studentId, lessonIds, requesterId);
        Map<UUID, List<com.example.interhubdev.absencenotice.StudentNoticeSummaryDto>> noticesByLesson =
                noticeApi.getNoticesByStudentAndLessons(studentId, lessonIds);

        List<StudentLessonAttendanceItemDto> items = new ArrayList<>();
        for (var item : records.items()) {
            List<com.example.interhubdev.absencenotice.StudentNoticeSummaryDto> notices =
                    noticesByLesson.getOrDefault(item.lessonSessionId(), List.of());
            items.add(new StudentLessonAttendanceItemDto(
                    item.lessonSessionId(),
                    item.record(),
                    notices
            ));
        }
        return new StudentAttendanceByLessonsDto(items);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupAttendanceSummaryDto getGroupAttendanceSummary(
            UUID groupId,
            LocalDate from,
            LocalDate to,
            UUID offeringId,
            UUID requesterId
    ) {
        return recordApi.getGroupAttendanceSummary(groupId, from, to, offeringId, requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherAbsenceNoticePage getTeacherAbsenceNotices(
            UUID teacherId,
            List<AbsenceNoticeStatus> statuses,
            UUID cursor,
            Integer limit
    ) {
        return noticeApi.getTeacherAbsenceNotices(teacherId, statuses, cursor, limit);
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
        return noticeApi.getMyAbsenceNotices(studentId, from, to, cursor, limit);
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
