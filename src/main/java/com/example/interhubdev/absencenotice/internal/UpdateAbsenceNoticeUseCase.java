package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.absencenotice.AbsenceNoticeStatus;
import com.example.interhubdev.absencenotice.SubmitAbsenceNoticeRequest;
import com.example.interhubdev.absencenotice.internal.integration.AbsenceNoticeUpdatedEventPayload;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
class UpdateAbsenceNoticeUseCase {

    private static final int MAX_ATTACHMENTS = 10;

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final OutboxIntegrationEventPublisher publisher;

    AbsenceNoticeDto execute(UUID noticeId, SubmitAbsenceNoticeRequest request, UUID studentId) {
        AbsenceNotice notice = noticeRepository.findByIdAndStudentId(noticeId, studentId)
                .orElseThrow(() -> AbsenceNoticeErrors.noticeNotFound(noticeId));

        if (notice.getStatus() != AbsenceNoticeStatus.SUBMITTED) {
            throw AbsenceNoticeErrors.noticeCannotBeUpdatedAfterResponse(noticeId);
        }

        if (!notice.getLessonSessionId().equals(request.lessonSessionId())) {
            throw AbsenceNoticeErrors.validationFailed("lessonSessionId cannot be changed");
        }

        List<String> fileIds = request.fileIds() != null ? request.fileIds() : List.of();
        if (fileIds.size() > MAX_ATTACHMENTS) {
            throw AbsenceNoticeErrors.invalidAttachmentCount(fileIds.size(), MAX_ATTACHMENTS);
        }
        for (String fileId : fileIds) {
            if (fileId == null || fileId.isBlank()) {
                throw AbsenceNoticeErrors.invalidFileId(fileId, "File ID cannot be blank");
            }
            try {
                UUID.fromString(fileId);
            } catch (IllegalArgumentException e) {
                throw AbsenceNoticeErrors.invalidFileId(fileId, "File ID must be a valid UUID");
            }
        }

        notice.setType(request.type());
        notice.setReasonText(request.reasonText() != null && !request.reasonText().isBlank() ? request.reasonText() : null);

        AbsenceNotice saved = noticeRepository.save(notice);

        List<AbsenceNoticeAttachment> currentAttachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);
        List<String> currentFileIds = currentAttachments.stream().map(AbsenceNoticeAttachment::getFileId).toList();
        List<String> toDelete = currentFileIds.stream().filter(fid -> !fileIds.contains(fid)).toList();
        if (!toDelete.isEmpty()) {
            attachmentRepository.deleteByNoticeIdAndFileIdIn(noticeId, toDelete);
        }

        List<String> existingFileIds = currentAttachments.stream().map(AbsenceNoticeAttachment::getFileId).toList();
        List<AbsenceNoticeAttachment> toCreate = new ArrayList<>();
        for (String fileId : fileIds) {
            if (!existingFileIds.contains(fileId)) {
                toCreate.add(AbsenceNoticeAttachment.builder().noticeId(noticeId).fileId(fileId).build());
            }
        }
        if (!toCreate.isEmpty()) {
            attachmentRepository.saveAll(toCreate);
        }

        List<AbsenceNoticeAttachment> finalAttachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);

        Instant occurredAt = Instant.now();
        AbsenceNoticeUpdatedEventPayload payload = new AbsenceNoticeUpdatedEventPayload(
                saved.getId(),
                saved.getLessonSessionId(),
                saved.getStudentId(),
                saved.getType(),
                toInstant(saved.getUpdatedAt())
        );
        publisher.publish(OutboxEventDraft.builder()
                .eventType(AbsenceNoticeEventTypes.ABSENCE_NOTICE_UPDATED)
                .payload(payload)
                .occurredAt(occurredAt)
                .build());

        return AbsenceNoticeMappers.toDto(saved, finalAttachments);
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : Instant.now();
    }
}
