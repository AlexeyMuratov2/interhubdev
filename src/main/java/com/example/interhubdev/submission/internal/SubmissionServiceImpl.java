package com.example.interhubdev.submission.internal;

import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.submission.SubmissionApi;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
        // File existence is validated by the controller (document API) to avoid circular dependency.

        HomeworkSubmission submission = HomeworkSubmission.builder()
            .homeworkId(homeworkId)
            .authorId(requesterId)
            .description(description)
            .build();

        try {
            HomeworkSubmission saved = submissionRepository.save(submission);
            int order = 0;
            for (UUID fileId : fileIds) {
                HomeworkSubmissionFile f = new HomeworkSubmissionFile();
                f.setSubmissionId(saved.getId());
                f.setStoredFileId(fileId);
                f.setSortOrder(order++);
                f.setSubmission(saved);
                saved.getFiles().add(f);
                submissionFileRepository.save(f);
            }
            return toDto(saved);
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
    public Optional<HomeworkSubmissionDto> get(UUID submissionId, UUID requesterId) {
        validateRequester(requesterId);
        checkTeacherOrAdmin(requesterId);

        return submissionRepository.findByIdWithFiles(submissionId)
            .map(this::toDto);
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
