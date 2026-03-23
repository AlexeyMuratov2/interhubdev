package com.example.interhubdev.submission.internal;

import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.fileasset.FileAssetApi;
import com.example.interhubdev.fileasset.FileAssetDownloadHandle;
import com.example.interhubdev.fileasset.FileAssetUploadCommand;
import com.example.interhubdev.fileasset.FileAssetView;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.submission.SubmissionAttachmentApi;
import com.example.interhubdev.submission.SubmissionAttachmentDto;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class SubmissionAttachmentService implements SubmissionAttachmentApi {

    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final FileAssetApi fileAssetApi;
    private final UserApi userApi;
    private final HomeworkApi homeworkApi;
    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final TeacherApi teacherApi;

    List<SubmissionAttachmentDto> createAttachments(UUID submissionId, List<FileAssetUploadCommand> uploads) {
        if (uploads == null || uploads.isEmpty()) {
            return List.of();
        }
        int nextOrder = submissionAttachmentRepository.findBySubmissionIdOrderBySortOrderAsc(submissionId).size();
        List<SubmissionAttachment> created = new java.util.ArrayList<>(uploads.size());
        List<UUID> fileAssetIds = new java.util.ArrayList<>(uploads.size());
        for (FileAssetUploadCommand upload : uploads) {
            FileAssetView asset = fileAssetApi.ingest(upload);
            fileAssetIds.add(asset.id());
            created.add(submissionAttachmentRepository.save(SubmissionAttachment.builder()
                .id(UUID.randomUUID())
                .submissionId(submissionId)
                .fileAssetId(asset.id())
                .sortOrder(nextOrder++)
                .build()));
        }
        confirmAfterCommit(fileAssetIds);
        return mapDtos(created);
    }

    void removeAll(UUID submissionId) {
        List<SubmissionAttachment> attachments = submissionAttachmentRepository.findBySubmissionIdOrderBySortOrderAsc(submissionId);
        if (attachments.isEmpty()) {
            return;
        }
        submissionAttachmentRepository.deleteAll(attachments);
        attachments.forEach(attachment -> deleteAfterCommit(attachment.getFileAssetId()));
    }

    List<SubmissionAttachmentDto> findBySubmission(UUID submissionId) {
        return mapDtos(submissionAttachmentRepository.findBySubmissionIdOrderBySortOrderAsc(submissionId));
    }

    Map<UUID, List<SubmissionAttachmentDto>> findBySubmissionIds(Collection<UUID> submissionIds) {
        if (submissionIds == null || submissionIds.isEmpty()) {
            return Map.of();
        }
        List<SubmissionAttachment> attachments = submissionAttachmentRepository.findBySubmissionIdInOrderBySubmissionIdAndSortOrder(submissionIds);
        Map<UUID, List<SubmissionAttachment>> grouped = attachments.stream()
            .collect(Collectors.groupingBy(SubmissionAttachment::getSubmissionId, LinkedHashMap::new, Collectors.toList()));
        Set<UUID> fileAssetIds = attachments.stream().map(SubmissionAttachment::getFileAssetId).collect(Collectors.toSet());
        Map<UUID, FileAssetView> assets = fileAssetApi.getMany(fileAssetIds);

        Map<UUID, List<SubmissionAttachmentDto>> result = new LinkedHashMap<>();
        for (UUID submissionId : submissionIds) {
            List<SubmissionAttachment> ownerAttachments = grouped.getOrDefault(submissionId, List.of());
            result.put(submissionId, ownerAttachments.stream()
                .map(attachment -> SubmissionAttachmentMapper.toDto(attachment, assets.get(attachment.getFileAssetId())))
                .toList());
        }
        return result;
    }

    List<SubmissionAttachment> findRawBySubmission(UUID submissionId) {
        return submissionAttachmentRepository.findBySubmissionIdOrderBySortOrderAsc(submissionId);
    }

    @Override
    public java.util.Optional<SubmissionAttachmentDto> get(UUID attachmentId, UUID requesterId) {
        SubmissionAttachment attachment = submissionAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> Errors.notFound("Submission attachment not found: " + attachmentId));
        HomeworkSubmission submission = submissionRepository.findById(attachment.getSubmissionId())
            .orElseThrow(() -> Errors.notFound("Submission not found: " + attachment.getSubmissionId()));
        ensureCanAccess(submission, requesterId);
        FileAssetView asset = fileAssetApi.get(attachment.getFileAssetId())
            .orElseThrow(() -> Errors.notFound("File asset not found for attachment " + attachmentId));
        return java.util.Optional.of(SubmissionAttachmentMapper.toDto(attachment, asset));
    }

    @Override
    public FileAssetDownloadHandle download(UUID attachmentId, UUID requesterId) {
        SubmissionAttachment attachment = submissionAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> Errors.notFound("Submission attachment not found: " + attachmentId));
        HomeworkSubmission submission = submissionRepository.findById(attachment.getSubmissionId())
            .orElseThrow(() -> Errors.notFound("Submission not found: " + attachment.getSubmissionId()));
        ensureCanAccess(submission, requesterId);
        return fileAssetApi.openDownload(attachment.getFileAssetId());
    }

    private List<SubmissionAttachmentDto> mapDtos(List<SubmissionAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        Set<UUID> fileAssetIds = attachments.stream().map(SubmissionAttachment::getFileAssetId).collect(Collectors.toSet());
        Map<UUID, FileAssetView> assets = fileAssetApi.getMany(fileAssetIds);
        return attachments.stream()
            .sorted(java.util.Comparator.comparingInt(SubmissionAttachment::getSortOrder))
            .map(attachment -> SubmissionAttachmentMapper.toDto(attachment, assets.get(attachment.getFileAssetId())))
            .toList();
    }

    private void ensureCanAccess(HomeworkSubmission submission, UUID requesterId) {
        UserDto user = userApi.findById(requesterId).orElseThrow(() -> Errors.unauthorized("Authentication required"));
        if (submission.getAuthorId().equals(requesterId)) {
            return;
        }
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        if (!user.hasRole(Role.TEACHER)) {
            throw Errors.forbidden("Insufficient permissions");
        }
        UUID homeworkId = submission.getHomeworkId();
        var homework = homeworkApi.get(homeworkId, requesterId).orElseThrow(() -> Errors.notFound("Homework not found: " + homeworkId));
        var lesson = scheduleApi.findLessonById(homework.lessonId()).orElseThrow(() -> Errors.notFound("Lesson not found: " + homework.lessonId()));
        var offering = offeringApi.findOfferingById(lesson.offeringId()).orElseThrow(() -> Errors.notFound("Offering not found: " + lesson.offeringId()));
        var teacher = teacherApi.findByUserId(requesterId).orElseThrow(() -> Errors.forbidden("Insufficient permissions"));
        if (offering.teacherId() != null && offering.teacherId().equals(teacher.id())) {
            return;
        }
        boolean assigned = offeringApi.findTeachersByOfferingId(offering.id()).stream()
            .anyMatch(item -> item.teacherId().equals(teacher.id()));
        if (!assigned) {
            throw Errors.forbidden("Insufficient permissions");
        }
    }

    private void confirmAfterCommit(List<UUID> fileAssetIds) {
        afterCommit(() -> {
            for (UUID fileAssetId : fileAssetIds) {
                try {
                    fileAssetApi.confirmBound(fileAssetId);
                } catch (Exception ignored) {
                    // Best-effort bind confirmation.
                }
            }
            return null;
        });
    }

    private void deleteAfterCommit(UUID fileAssetId) {
        afterCommit(() -> {
            try {
                fileAssetApi.markDeleted(fileAssetId);
            } catch (Exception ignored) {
                // Ignore "in use" / already deleted during cleanup.
            }
            return null;
        });
    }

    private static <T> T afterCommit(Supplier<T> afterCommitAction) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return afterCommitAction.get();
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                afterCommitAction.get();
            }
        });
        return null;
    }
}
