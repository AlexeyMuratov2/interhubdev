package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeDto;
import com.example.interhubdev.attendance.AbsenceNoticeStatus;
import com.example.interhubdev.attendance.SubmitAbsenceNoticeRequest;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.schedule.LessonDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Use case for submitting or updating absence notices with attachment synchronization.
 * Students can submit a notice for a lesson session or update an existing active notice.
 */
@Service
@RequiredArgsConstructor
@Transactional
class SubmitOrUpdateAbsenceNoticeUseCase {

    private static final int MAX_ATTACHMENTS = 10;

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final SessionGateway sessionGateway;
    private final RosterGateway rosterGateway;
    private final OfferingApi offeringApi;

    /**
     * Submit or update an absence notice for a student.
     *
     * @param request    submission request
     * @param studentId student profile ID
     * @return created or updated notice DTO
     * @throws com.example.interhubdev.error.AppException if session not found, student not in group, or validation fails
     */
    AbsenceNoticeDto execute(SubmitAbsenceNoticeRequest request, UUID studentId) {
        // Validate session exists
        LessonDto session = sessionGateway.getSessionById(request.lessonSessionId())
                .orElseThrow(() -> AttendanceErrors.sessionNotFound(request.lessonSessionId()));

        // Get offering to get groupId
        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(session.offeringId())
                .orElseThrow(() -> AttendanceErrors.offeringNotFound(session.offeringId()));

        // Validate student is in session's group roster
        if (!rosterGateway.isStudentInGroup(studentId, offering.groupId())) {
            throw AttendanceErrors.studentNotInGroup(studentId, offering.groupId());
        }

        // Validate attachment count
        List<String> fileIds = request.fileIds() != null ? request.fileIds() : List.of();
        if (fileIds.size() > MAX_ATTACHMENTS) {
            throw AttendanceErrors.invalidAttachmentCount(fileIds.size(), MAX_ATTACHMENTS);
        }

        // Validate file IDs format (basic UUID format check)
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

        // Find existing active notice for this student and session
        AbsenceNotice notice = noticeRepository
                .findActiveBySessionAndStudent(request.lessonSessionId(), studentId, AbsenceNoticeStatus.SUBMITTED)
                .orElse(null);

        if (notice != null) {
            // Update existing notice
            notice.setType(request.type());
            notice.setReasonText(request.reasonText() != null && !request.reasonText().isBlank()
                    ? request.reasonText() : null);
            // updatedAt is set by @PreUpdate
        } else {
            // Create new notice
            notice = AbsenceNotice.builder()
                    .lessonSessionId(request.lessonSessionId())
                    .studentId(studentId)
                    .type(request.type())
                    .reasonText(request.reasonText() != null && !request.reasonText().isBlank()
                            ? request.reasonText() : null)
                    .status(AbsenceNoticeStatus.SUBMITTED)
                    .build();
        }

        AbsenceNotice saved = noticeRepository.save(notice);

        // Synchronize attachments: replace all attachments with new list
        UUID noticeId = saved.getId();
        List<AbsenceNoticeAttachment> currentAttachments = attachmentRepository
                .findByNoticeIdOrderByCreatedAtAsc(noticeId);

        // Delete attachments that are not in the new list
        List<String> currentFileIds = currentAttachments.stream()
                .map(AbsenceNoticeAttachment::getFileId)
                .toList();
        List<String> toDelete = currentFileIds.stream()
                .filter(fid -> !fileIds.contains(fid))
                .toList();
        if (!toDelete.isEmpty()) {
            attachmentRepository.deleteByNoticeIdAndFileIdIn(noticeId, toDelete);
        }

        // Create new attachments for file IDs that don't exist yet
        List<String> existingFileIds = currentAttachments.stream()
                .map(AbsenceNoticeAttachment::getFileId)
                .toList();
        List<AbsenceNoticeAttachment> toCreate = new ArrayList<>();
        for (String fileId : fileIds) {
            if (!existingFileIds.contains(fileId)) {
                toCreate.add(AbsenceNoticeAttachment.builder()
                        .noticeId(noticeId)
                        .fileId(fileId)
                        .build());
            }
        }
        if (!toCreate.isEmpty()) {
            attachmentRepository.saveAll(toCreate);
        }

        // Load final attachments
        List<AbsenceNoticeAttachment> finalAttachments = attachmentRepository
                .findByNoticeIdOrderByCreatedAtAsc(noticeId);

        // TODO: publish IntegrationEvent AbsenceNoticeSubmittedOrUpdated {
        //   noticeId, sessionId, studentId, type, submittedAt/updatedAt
        // }
        // TODO: notify teachers of session via Notification module (future outbox)

        return AbsenceNoticeMappers.toDto(saved, finalAttachments);
    }
}
