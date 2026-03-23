package com.example.interhubdev.submission.internal;

import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.fileasset.FileAssetUploadCommand;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.subject.SubjectDto;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.submission.SubmissionApi;
import com.example.interhubdev.submission.SubmissionsArchiveHandle;
import com.example.interhubdev.submission.internal.archive.ArchiveData;
import com.example.interhubdev.submission.internal.archive.ArchiveEntry;
import com.example.interhubdev.submission.internal.archive.ArchiveInfo;
import com.example.interhubdev.submission.internal.archive.ArchiveNamingService;
import com.example.interhubdev.submission.internal.integration.HomeworkSubmissionSubmittedEventPayload;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link SubmissionApi}: create, list, get, delete submissions.
 * Create only for students; list/get for teachers and admins; delete only for author.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class SubmissionServiceImpl implements SubmissionApi {

    private static final int MAX_DESCRIPTION_LENGTH = 5000;

    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkApi homeworkApi;
    private final UserApi userApi;
    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final TeacherApi teacherApi;
    private final ProgramApi programApi;
    private final SubjectApi subjectApi;
    private final SubmissionAttachmentService submissionAttachmentService;
    private final OutboxIntegrationEventPublisher outboxPublisher;

    @Override
    @Transactional
    public HomeworkSubmissionDto create(UUID homeworkId, String description, List<FileAssetUploadCommand> uploads, UUID requesterId) {
        validateRequester(requesterId);
        checkStudentRole(requesterId);

        HomeworkDto homework = homeworkApi.get(homeworkId, requesterId)
                .orElseThrow(() -> SubmissionErrors.homeworkNotFound(homeworkId));
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw SubmissionErrors.validationFailed("Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }

        // One submission per student per homework: replace any existing submission by this author.
        List<HomeworkSubmission> existing = submissionRepository.findByHomeworkIdAndAuthorId(homeworkId, requesterId);
        for (HomeworkSubmission old : existing) {
            submissionAttachmentService.removeAll(old.getId());
            submissionRepository.delete(old);
        }

        HomeworkSubmission submission = HomeworkSubmission.builder()
            .homeworkId(homeworkId)
            .authorId(requesterId)
            .description(description)
            .build();

        try {
            HomeworkSubmission saved = submissionRepository.save(submission);
            var attachments = submissionAttachmentService.createAttachments(saved.getId(), uploads);

            HomeworkSubmissionSubmittedEventPayload eventPayload = new HomeworkSubmissionSubmittedEventPayload(
                    saved.getId(),
                    homeworkId,
                    homework.lessonId(),
                    requesterId,
                    saved.getSubmittedAt().atZone(ZoneOffset.UTC).toInstant()
            );
            outboxPublisher.publish(OutboxEventDraft.builder()
                    .eventType(SubmissionEventTypes.HOMEWORK_SUBMISSION_SUBMITTED)
                    .payload(eventPayload)
                    .occurredAt(eventPayload.submittedAt())
                    .build());

            return SubmissionMappers.toDto(saved, attachments);
        } catch (PersistenceException | DataIntegrityViolationException e) {
            log.warn("Failed to save submission (homeworkId={}): {}", homeworkId, e.getMessage());
            throw SubmissionErrors.saveFailed();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkSubmissionDto> listByHomework(UUID homeworkId, UUID requesterId) {
        validateRequester(requesterId);
        checkTeacherOrAdmin(requesterId);

        if (homeworkApi.get(homeworkId, requesterId).isEmpty()) {
            throw SubmissionErrors.homeworkNotFound(homeworkId);
        }
        List<HomeworkSubmission> list = submissionRepository.findByHomeworkIdOrderBySubmittedAtDesc(homeworkId);
        var attachmentsBySubmission = submissionAttachmentService.findBySubmissionIds(list.stream().map(HomeworkSubmission::getId).toList());
        return list.stream()
            .map(submission -> SubmissionMappers.toDto(submission, attachmentsBySubmission.getOrDefault(submission.getId(), List.of())))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkSubmissionDto> listByHomeworkIds(Collection<UUID> homeworkIds, UUID requesterId) {
        validateRequester(requesterId);
        if (homeworkIds == null || homeworkIds.isEmpty()) {
            return List.of();
        }
        List<HomeworkSubmission> list = submissionRepository.findByHomeworkIdIn(homeworkIds);
        if (!isTeacherOrAdmin(requesterId)) {
            list = list.stream().filter(s -> s.getAuthorId().equals(requesterId)).toList();
        }
        var attachmentsBySubmission = submissionAttachmentService.findBySubmissionIds(list.stream().map(HomeworkSubmission::getId).toList());
        return list.stream()
            .map(submission -> SubmissionMappers.toDto(submission, attachmentsBySubmission.getOrDefault(submission.getId(), List.of())))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HomeworkSubmissionDto> get(UUID submissionId, UUID requesterId) {
        validateRequester(requesterId);
        checkTeacherOrAdmin(requesterId);

        return submissionRepository.findByIdWithFiles(submissionId)
            .map(submission -> SubmissionMappers.toDto(
                submission,
                submissionAttachmentService.findBySubmission(submission.getId())
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkSubmissionDto> getByIds(Collection<UUID> submissionIds, UUID requesterId) {
        validateRequester(requesterId);
        if (submissionIds == null || submissionIds.isEmpty()) {
            return List.of();
        }
        List<HomeworkSubmission> list = submissionRepository.findByIdInWithFiles(submissionIds);
        if (!isTeacherOrAdmin(requesterId)) {
            list = list.stream().filter(s -> s.getAuthorId().equals(requesterId)).toList();
        }
        var attachmentsBySubmission = submissionAttachmentService.findBySubmissionIds(list.stream().map(HomeworkSubmission::getId).toList());
        return list.stream()
            .map(submission -> SubmissionMappers.toDto(submission, attachmentsBySubmission.getOrDefault(submission.getId(), List.of())))
            .toList();
    }

    @Override
    @Transactional
    public void delete(UUID submissionId, UUID requesterId) {
        validateRequester(requesterId);

        HomeworkSubmission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> SubmissionErrors.submissionNotFound(submissionId));
        if (!submission.getAuthorId().equals(requesterId)) {
            throw SubmissionErrors.permissionDenied();
        }
        submissionAttachmentService.removeAll(submissionId);
        submissionRepository.delete(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public int countSubmittedByAuthorForHomeworkIds(UUID authorId, Collection<UUID> homeworkIds, UUID requesterId) {
        validateRequester(requesterId);
        if (!authorId.equals(requesterId)) {
            checkTeacherOrAdmin(requesterId);
        }
        if (homeworkIds == null || homeworkIds.isEmpty()) {
            return 0;
        }
        return (int) submissionRepository.countDistinctHomeworkIdsByAuthorIdAndHomeworkIdIn(authorId, homeworkIds);
    }

    @Override
    public SubmissionsArchiveHandle buildSubmissionsArchive(UUID homeworkId, UUID requesterId) {
        validateRequester(requesterId);
        ensureCanDownloadArchiveByHomework(homeworkId, requesterId);
        ArchiveData data = loadArchiveData(homeworkId, requesterId);
        String filename = ArchiveNamingService.buildArchiveFilename(data.info());
        return new SubmissionsArchiveHandleImpl(filename, data, requesterId, submissionAttachmentService);
    }

    /**
     * Ensure requester is teacher of the lesson for this homework or admin/moderator.
     */
    private void ensureCanDownloadArchiveByHomework(UUID homeworkId, UUID requesterId) {
        UserDto user = userApi.findById(requesterId).orElseThrow(SubmissionErrors::permissionDenied);
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        if (!user.hasRole(Role.TEACHER)) {
            throw SubmissionErrors.permissionDenied();
        }
        HomeworkDto homework = homeworkApi.get(homeworkId, requesterId)
            .orElseThrow(() -> SubmissionErrors.homeworkNotFound(homeworkId));
        var lessonOpt = scheduleApi.findLessonById(homework.lessonId());
        if (lessonOpt.isEmpty()) {
            throw SubmissionErrors.homeworkNotFound(homeworkId);
        }
        var offeringOpt = offeringApi.findOfferingById(lessonOpt.get().offeringId());
        if (offeringOpt.isEmpty()) {
            throw SubmissionErrors.homeworkNotFound(homeworkId);
        }
        var teacherOpt = teacherApi.findByUserId(requesterId);
        if (teacherOpt.isEmpty()) {
            throw SubmissionErrors.permissionDenied();
        }
        UUID teacherId = teacherOpt.get().id();
        var offering = offeringOpt.get();
        if (offering.teacherId() != null && offering.teacherId().equals(teacherId)) {
            return;
        }
        boolean assigned = offeringApi.findTeachersByOfferingId(offering.id()).stream()
            .anyMatch(t -> t.teacherId().equals(teacherId));
        if (!assigned) {
            throw SubmissionErrors.permissionDenied();
        }
    }

    /**
     * Load archive metadata and list of file entries. Runs in read-only transaction.
     */
    @Transactional(readOnly = true)
    public ArchiveData loadArchiveData(UUID homeworkId, UUID requesterId) {
        HomeworkDto homework = homeworkApi.get(homeworkId, requesterId)
            .orElseThrow(() -> SubmissionErrors.homeworkNotFound(homeworkId));
        var lessonOpt = scheduleApi.findLessonById(homework.lessonId());
        if (lessonOpt.isEmpty()) {
            throw SubmissionErrors.homeworkNotFound(homeworkId);
        }
        var lesson = lessonOpt.get();
        LocalDate lessonDate = lesson.date();
        String subjectName = "Subject";
        var offeringOpt = offeringApi.findOfferingById(lesson.offeringId());
        if (offeringOpt.isPresent()) {
            var curriculumOpt = programApi.findCurriculumSubjectById(offeringOpt.get().curriculumSubjectId());
            if (curriculumOpt.isPresent()) {
                var subjectOpt = subjectApi.findSubjectById(curriculumOpt.get().subjectId());
                if (subjectOpt.isPresent()) {
                    subjectName = subjectDisplayName(subjectOpt.get());
                }
            }
        }
        ArchiveInfo info = new ArchiveInfo(subjectName, homework.title(), lessonDate);

        List<HomeworkSubmission> submissions = submissionRepository.findByHomeworkIdOrderBySubmittedAtDesc(homeworkId);
        var attachmentsBySubmission = submissionAttachmentService.findBySubmissionIds(
            submissions.stream().map(HomeworkSubmission::getId).toList()
        );
        Set<UUID> authorIds = submissions.stream().map(HomeworkSubmission::getAuthorId).collect(Collectors.toSet());
        List<UserDto> users = userApi.findByIds(authorIds);
        var userMap = users.stream().collect(Collectors.toMap(UserDto::id, u -> u.getFullName()));

        List<ArchiveEntry> entries = new ArrayList<>();
        for (HomeworkSubmission s : submissions) {
            String studentName = userMap.getOrDefault(s.getAuthorId(), s.getAuthorId().toString());
            List<com.example.interhubdev.submission.SubmissionAttachmentDto> files = attachmentsBySubmission.getOrDefault(s.getId(), List.of());
            int fileIndex = 0;
            for (com.example.interhubdev.submission.SubmissionAttachmentDto f : files) {
                String originalName = f.fileName();
                String extension = extensionFromFilename(originalName);
                entries.add(new ArchiveEntry(
                    s.getAuthorId(),
                    studentName,
                    homework.title(),
                    lessonDate,
                    f.id(),
                    originalName != null ? originalName : "",
                    extension,
                    fileIndex++
                ));
            }
        }
        return new ArchiveData(info, entries);
    }

    private static String subjectDisplayName(SubjectDto subject) {
        if (subject == null) return "Subject";
        if (subject.englishName() != null && !subject.englishName().isBlank()) {
            return subject.englishName();
        }
        return subject.chineseName() != null && !subject.chineseName().isBlank() ? subject.chineseName() : "Subject";
    }

    private static String extensionFromFilename(String filename) {
        if (filename == null || filename.isBlank()) return "";
        int i = filename.lastIndexOf('.');
        return i > 0 && i < filename.length() - 1 ? filename.substring(i + 1) : "";
    }

    private void validateRequester(UUID requesterId) {
        userApi.findById(requesterId)
            .orElseThrow(() -> Errors.unauthorized("Authentication required"));
    }

    private void checkStudentRole(UUID userId) {
        UserDto user = userApi.findById(userId)
            .orElseThrow(SubmissionErrors::permissionDenied);
        if (!user.hasRole(Role.STUDENT)) {
            throw SubmissionErrors.permissionDenied();
        }
    }

    private void checkTeacherOrAdmin(UUID userId) {
        UserDto user = userApi.findById(userId)
            .orElseThrow(SubmissionErrors::permissionDenied);
        if (user.hasRole(Role.TEACHER) || user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        throw SubmissionErrors.permissionDenied();
    }

    private boolean isTeacherOrAdmin(UUID userId) {
        UserDto user = userApi.findById(userId).orElse(null);
        return user != null && (user.hasRole(Role.TEACHER) || user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN));
    }
}
