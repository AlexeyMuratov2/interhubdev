package com.example.interhubdev.submission.internal;

import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.subject.SubjectDto;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.submission.SubmissionApi;
import com.example.interhubdev.submission.internal.archive.ArchiveData;
import com.example.interhubdev.submission.internal.archive.ArchiveEntry;
import com.example.interhubdev.submission.internal.archive.ArchiveInfo;
import com.example.interhubdev.submission.SubmissionsArchiveHandle;
import com.example.interhubdev.submission.internal.archive.ArchiveNamingService;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final HomeworkSubmissionFileRepository submissionFileRepository;
    private final HomeworkApi homeworkApi;
    private final UserApi userApi;
    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final TeacherApi teacherApi;
    private final ProgramApi programApi;
    private final SubjectApi subjectApi;
    private final DocumentApi documentApi;

    @Override
    @Transactional
    public HomeworkSubmissionDto create(UUID homeworkId, String description, List<UUID> storedFileIds, UUID requesterId) {
        validateRequester(requesterId);
        checkStudentRole(requesterId);

        if (homeworkApi.get(homeworkId, requesterId).isEmpty()) {
            throw SubmissionErrors.homeworkNotFound(homeworkId);
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw SubmissionErrors.validationFailed("Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }

        List<UUID> fileIds = storedFileIds != null ? storedFileIds : List.of();
        List<UUID> distinctFileIds = fileIds.stream().distinct().toList();
        if (distinctFileIds.size() != fileIds.size()) {
            throw SubmissionErrors.validationFailed("Duplicate file IDs are not allowed");
        }
        // File existence is validated by the controller (document API) to avoid circular dependency.

        // One submission per student per homework: replace any existing submission by this author.
        List<HomeworkSubmission> existing = submissionRepository.findByHomeworkIdAndAuthorId(homeworkId, requesterId);
        for (HomeworkSubmission old : existing) {
            submissionRepository.delete(old);
        }

        HomeworkSubmission submission = HomeworkSubmission.builder()
            .homeworkId(homeworkId)
            .authorId(requesterId)
            .description(description)
            .build();

        try {
            HomeworkSubmission saved = submissionRepository.save(submission);
            for (int i = 0; i < distinctFileIds.size(); i++) {
                HomeworkSubmissionFile f = new HomeworkSubmissionFile();
                f.setSubmissionId(saved.getId());
                f.setStoredFileId(distinctFileIds.get(i));
                f.setSortOrder(i);
                submissionFileRepository.save(f);
            }
            HomeworkSubmission withFiles = submissionRepository.findByIdWithFiles(saved.getId())
                .orElse(saved);
            return toDto(withFiles);
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
        return list.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkSubmissionDto> listByHomeworkIds(Collection<UUID> homeworkIds, UUID requesterId) {
        validateRequester(requesterId);
        checkTeacherOrAdmin(requesterId);
        if (homeworkIds == null || homeworkIds.isEmpty()) {
            return List.of();
        }
        List<HomeworkSubmission> list = submissionRepository.findByHomeworkIdIn(homeworkIds);
        return list.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HomeworkSubmissionDto> get(UUID submissionId, UUID requesterId) {
        validateRequester(requesterId);
        checkTeacherOrAdmin(requesterId);

        return submissionRepository.findByIdWithFiles(submissionId)
            .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkSubmissionDto> getByIds(Collection<UUID> submissionIds, UUID requesterId) {
        validateRequester(requesterId);
        checkTeacherOrAdmin(requesterId);
        if (submissionIds == null || submissionIds.isEmpty()) {
            return List.of();
        }
        List<HomeworkSubmission> list = submissionRepository.findByIdInWithFiles(submissionIds);
        return list.stream().map(this::toDto).toList();
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
        submissionRepository.delete(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStoredFileInUse(UUID storedFileId) {
        return submissionFileRepository.existsByStoredFileId(storedFileId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canTeacherDownloadSubmissionFile(UUID storedFileId, UUID userId) {
        List<HomeworkSubmissionFile> files = submissionFileRepository.findByStoredFileId(storedFileId);
        if (files.isEmpty()) {
            return false;
        }
        HomeworkSubmissionFile first = files.get(0);
        Optional<HomeworkSubmission> submissionOpt = submissionRepository.findByIdWithFiles(first.getSubmissionId());
        if (submissionOpt.isEmpty()) {
            return false;
        }
        UUID homeworkId = submissionOpt.get().getHomeworkId();
        Optional<HomeworkDto> homeworkOpt = homeworkApi.get(homeworkId, userId);
        if (homeworkOpt.isEmpty()) {
            return false;
        }
        UUID lessonId = homeworkOpt.get().lessonId();
        var lessonOpt = scheduleApi.findLessonById(lessonId);
        if (lessonOpt.isEmpty()) {
            return false;
        }
        var offeringOpt = offeringApi.findOfferingById(lessonOpt.get().offeringId());
        if (offeringOpt.isEmpty()) {
            return false;
        }
        var teacherOpt = teacherApi.findByUserId(userId);
        if (teacherOpt.isEmpty()) {
            return false;
        }
        UUID teacherId = teacherOpt.get().id();
        var offering = offeringOpt.get();
        if (offering.teacherId() != null && offering.teacherId().equals(teacherId)) {
            return true;
        }
        return offeringApi.findTeachersByOfferingId(offering.id()).stream()
            .anyMatch(t -> t.teacherId().equals(teacherId));
    }

    @Override
    public SubmissionsArchiveHandle buildSubmissionsArchive(UUID homeworkId, UUID requesterId) {
        validateRequester(requesterId);
        ensureCanDownloadArchiveByHomework(homeworkId, requesterId);
        ArchiveData data = loadArchiveData(homeworkId, requesterId);
        String filename = ArchiveNamingService.buildArchiveFilename(data.info());
        return new SubmissionsArchiveHandleImpl(filename, data, requesterId, documentApi);
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
        Set<UUID> authorIds = submissions.stream().map(HomeworkSubmission::getAuthorId).collect(Collectors.toSet());
        List<UserDto> users = userApi.findByIds(authorIds);
        var userMap = users.stream().collect(Collectors.toMap(UserDto::id, u -> u.getFullName()));

        List<ArchiveEntry> entries = new ArrayList<>();
        for (HomeworkSubmission s : submissions) {
            String studentName = userMap.getOrDefault(s.getAuthorId(), s.getAuthorId().toString());
            List<HomeworkSubmissionFile> files = s.getFiles() != null ? s.getFiles() : List.of();
            int fileIndex = 0;
            for (HomeworkSubmissionFile f : files) {
                String originalName = null;
                String extension = "";
                Optional<StoredFileDto> meta = documentApi.getStoredFile(f.getStoredFileId());
                if (meta.isPresent()) {
                    originalName = meta.get().originalName();
                    extension = extensionFromFilename(originalName);
                }
                entries.add(new ArchiveEntry(
                    s.getAuthorId(),
                    studentName,
                    homework.title(),
                    lessonDate,
                    f.getStoredFileId(),
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

    private HomeworkSubmissionDto toDto(HomeworkSubmission s) {
        List<UUID> ids = s.getFiles() != null
            ? s.getFiles().stream().map(HomeworkSubmissionFile::getStoredFileId).collect(Collectors.toList())
            : List.of();
        return SubmissionMappers.toDto(s, ids);
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
}
