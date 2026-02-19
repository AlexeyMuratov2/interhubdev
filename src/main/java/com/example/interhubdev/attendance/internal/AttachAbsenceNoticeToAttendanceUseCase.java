package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeStatus;
import com.example.interhubdev.attendance.AttendanceRecordDto;
import com.example.interhubdev.attendance.internal.integration.AbsenceNoticeAttachedEventPayload;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case for teachers to explicitly attach an absence notice to an attendance record.
 * Teacher must have permission to manage the session of the record.
 */
@Service
@RequiredArgsConstructor
@Transactional
class AttachAbsenceNoticeToAttendanceUseCase {

    private final AttendanceRecordRepository recordRepository;
    private final AbsenceNoticeRepository noticeRepository;
    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final AttendanceAccessPolicy accessPolicy;
    private final OutboxIntegrationEventPublisher publisher;

    /**
     * Attach an absence notice to an attendance record.
     *
     * @param recordId  attendance record ID
     * @param noticeId  absence notice ID to attach
     * @param requesterId user ID of requester (must be teacher of session or admin)
     * @return updated attendance record DTO
     * @throws com.example.interhubdev.error.AppException if record/notice not found, mismatch, or access denied
     */
    AttendanceRecordDto execute(UUID recordId, UUID noticeId, UUID requesterId) {
        // Load record
        AttendanceRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> AttendanceErrors.recordNotFound(recordId));

        // Check authorization: teacher must be able to manage the session
        LessonDto lesson = scheduleApi.findLessonById(record.getLessonSessionId())
                .orElseThrow(() -> AttendanceErrors.lessonNotFound(record.getLessonSessionId()));
        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> AttendanceErrors.offeringNotFound(lesson.offeringId()));
        accessPolicy.ensureCanManageSession(requesterId, lesson);

        // Load notice
        AbsenceNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> AttendanceErrors.noticeNotFound(noticeId));

        // Validate notice matches record
        if (!notice.getLessonSessionId().equals(record.getLessonSessionId())) {
            throw AttendanceErrors.noticeDoesNotMatchRecord(
                    noticeId, recordId, "notice sessionId does not match record sessionId");
        }
        if (!notice.getStudentId().equals(record.getStudentId())) {
            throw AttendanceErrors.noticeDoesNotMatchRecord(
                    noticeId, recordId, "notice studentId does not match record studentId");
        }

        // Validate notice is not canceled
        if (notice.getStatus() == AbsenceNoticeStatus.CANCELED) {
            throw AttendanceErrors.noticeCanceled(noticeId);
        }

        // Attach notice
        record.setAbsenceNoticeId(noticeId);
        AttendanceRecord saved = recordRepository.save(record);

        // Publish integration event
        Instant occurredAt = Instant.now();
        AbsenceNoticeAttachedEventPayload payload = new AbsenceNoticeAttachedEventPayload(
                saved.getId(),
                noticeId,
                saved.getLessonSessionId(),
                saved.getStudentId(),
                requesterId,
                occurredAt
        );
        publisher.publish(OutboxEventDraft.builder()
                .eventType(AttendanceEventTypes.ABSENCE_NOTICE_ATTACHED)
                .payload(payload)
                .occurredAt(occurredAt)
                .build());

        return AttendanceMappers.toDto(saved);
    }
}
