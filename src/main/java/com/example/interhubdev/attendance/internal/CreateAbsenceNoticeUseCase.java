package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeDto;
import com.example.interhubdev.attendance.AbsenceNoticeStatus;
import com.example.interhubdev.attendance.SubmitAbsenceNoticeRequest;
import com.example.interhubdev.attendance.internal.integration.AbsenceNoticeSubmittedEventPayload;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import com.example.interhubdev.schedule.LessonDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Use case for creating a new absence notice.
 * Students can create one notice per lesson session (only one active SUBMITTED per student/session).
 */
@Service
@RequiredArgsConstructor
@Transactional
class CreateAbsenceNoticeUseCase {

    private static final int MAX_ATTACHMENTS = 10;

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final SessionGateway sessionGateway;
    private final RosterGateway rosterGateway;
    private final OfferingApi offeringApi;
    private final OutboxIntegrationEventPublisher publisher;

    /**
     * Create a new absence notice for a student.
     *
     * @param request   submission request
     * @param studentId student profile ID
     * @return created notice DTO
     * @throws com.example.interhubdev.error.AppException if session not found, student not in group, already has active notice, or validation fails
     */
    AbsenceNoticeDto execute(SubmitAbsenceNoticeRequest request, UUID studentId) {
        LessonDto session = sessionGateway.getSessionById(request.lessonSessionId())
                .orElseThrow(() -> AttendanceErrors.sessionNotFound(request.lessonSessionId()));

        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(session.offeringId())
                .orElseThrow(() -> AttendanceErrors.offeringNotFound(session.offeringId()));

        if (!rosterGateway.isStudentInGroup(studentId, offering.groupId())) {
            throw AttendanceErrors.studentNotInGroup(studentId, offering.groupId());
        }

        if (noticeRepository.findActiveBySessionAndStudent(request.lessonSessionId(), studentId, AbsenceNoticeStatus.SUBMITTED).isPresent()) {
            throw AttendanceErrors.noticeAlreadyExistsForSession(request.lessonSessionId(), studentId);
        }

        List<String> fileIds = request.fileIds() != null ? request.fileIds() : List.of();
        if (fileIds.size() > MAX_ATTACHMENTS) {
            throw AttendanceErrors.invalidAttachmentCount(fileIds.size(), MAX_ATTACHMENTS);
        }
        for (String fileId : fileIds) {
            if (fileId == null || fileId.isBlank()) {
                throw AttendanceErrors.invalidFileId(fileId, "File ID cannot be blank");
            }
            try {
                UUID.fromString(fileId);
            } catch (IllegalArgumentException e) {
                throw AttendanceErrors.invalidFileId(fileId, "File ID must be a valid UUID");
            }
        }

        AbsenceNotice notice = AbsenceNotice.builder()
                .lessonSessionId(request.lessonSessionId())
                .studentId(studentId)
                .type(request.type())
                .reasonText(request.reasonText() != null && !request.reasonText().isBlank() ? request.reasonText() : null)
                .status(AbsenceNoticeStatus.SUBMITTED)
                .build();

        AbsenceNotice saved = noticeRepository.save(notice);

        List<AbsenceNoticeAttachment> toCreate = new ArrayList<>();
        for (String fileId : fileIds) {
            toCreate.add(AbsenceNoticeAttachment.builder()
                    .noticeId(saved.getId())
                    .fileId(fileId)
                    .build());
        }
        if (!toCreate.isEmpty()) {
            attachmentRepository.saveAll(toCreate);
        }

        List<AbsenceNoticeAttachment> finalAttachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(saved.getId());

        Instant occurredAt = Instant.now();
        AbsenceNoticeSubmittedEventPayload payload = new AbsenceNoticeSubmittedEventPayload(
                saved.getId(),
                saved.getLessonSessionId(),
                saved.getStudentId(),
                saved.getType(),
                toInstant(saved.getSubmittedAt())
        );
        publisher.publish(OutboxEventDraft.builder()
                .eventType(AttendanceEventTypes.ABSENCE_NOTICE_SUBMITTED)
                .payload(payload)
                .occurredAt(occurredAt)
                .build());

        return AbsenceNoticeMappers.toDto(saved, finalAttachments);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : Instant.now();
    }
}
