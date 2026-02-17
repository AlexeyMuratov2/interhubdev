package com.example.interhubdev.document.internal.homework;

import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.LessonLookupPort;
import com.example.interhubdev.document.internal.storedFile.StoredFile;
import com.example.interhubdev.document.internal.storedFile.StoredFileRepository;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link HomeworkApi}: create, list, get, update, delete homework.
 * When file reference is cleared we only set it to null; we do not delete the stored file.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class HomeworkServiceImpl implements HomeworkApi {

    private final HomeworkRepository homeworkRepository;
    private final StoredFileRepository storedFileRepository;
    private final LessonLookupPort lessonLookupPort;
    private final UserApi userApi;

    @Override
    @Transactional
    public HomeworkDto create(UUID lessonId, String title, String description, Integer points,
                              UUID storedFileId, UUID requesterId) {
        validateRequester(requesterId);
        checkManagePermission(requesterId);

        if (!lessonLookupPort.existsById(lessonId)) {
            throw HomeworkErrors.lessonNotFound(lessonId);
        }
        validateTitle(title);
        validatePoints(points);

        StoredFile file = null;
        if (storedFileId != null) {
            file = storedFileRepository.findById(storedFileId)
                .orElseThrow(() -> HomeworkErrors.fileNotFound(storedFileId));
        }

        Homework homework = Homework.builder()
            .lessonId(lessonId)
            .title(title)
            .description(description)
            .points(points)
            .storedFile(file)
            .build();

        try {
            Homework saved = homeworkRepository.save(homework);
            return HomeworkMappers.toDto(saved);
        } catch (PersistenceException | DataIntegrityViolationException e) {
            log.warn("Failed to save homework (lessonId={}): {}", lessonId, e.getMessage());
            throw HomeworkErrors.saveFailed();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkDto> listByLesson(UUID lessonId, UUID requesterId) {
        validateRequester(requesterId);
        if (!lessonLookupPort.existsById(lessonId)) {
            throw HomeworkErrors.lessonNotFound(lessonId);
        }
        List<Homework> list = homeworkRepository.findByLessonIdOrderByCreatedAtDesc(lessonId);
        return list.stream().map(HomeworkMappers::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HomeworkDto> get(UUID homeworkId, UUID requesterId) {
        validateRequester(requesterId);
        return homeworkRepository.findById(homeworkId).map(HomeworkMappers::toDto);
    }

    @Override
    @Transactional
    public HomeworkDto update(UUID homeworkId, String title, String description, Integer points,
                              boolean clearFile, UUID storedFileId, UUID requesterId) {
        validateRequester(requesterId);
        checkManagePermission(requesterId);

        Homework homework = homeworkRepository.findById(homeworkId)
            .orElseThrow(() -> HomeworkErrors.homeworkNotFound(homeworkId));

        if (title != null) {
            validateTitle(title);
            homework.setTitle(title);
        }
        if (description != null) {
            homework.setDescription(description.length() > 5000 ? description.substring(0, 5000) : description);
        }
        if (points != null) {
            validatePoints(points);
            homework.setPoints(points);
        }

        if (clearFile) {
            homework.setStoredFile(null);
        } else if (storedFileId != null) {
            StoredFile file = storedFileRepository.findById(storedFileId)
                .orElseThrow(() -> HomeworkErrors.fileNotFound(storedFileId));
            homework.setStoredFile(file);
        }
        homework.setUpdatedAt(LocalDateTime.now());

        try {
            Homework saved = homeworkRepository.save(homework);
            return HomeworkMappers.toDto(saved);
        } catch (PersistenceException | DataIntegrityViolationException e) {
            log.warn("Failed to update homework {}: {}", homeworkId, e.getMessage());
            throw HomeworkErrors.saveFailed();
        }
    }

    @Override
    @Transactional
    public void delete(UUID homeworkId, UUID requesterId) {
        validateRequester(requesterId);
        checkManagePermission(requesterId);

        Homework homework = homeworkRepository.findById(homeworkId)
            .orElseThrow(() -> HomeworkErrors.homeworkNotFound(homeworkId));
        homeworkRepository.delete(homework);
        // We do not delete the stored file when homework is deleted
    }

    private void validateRequester(UUID requesterId) {
        userApi.findById(requesterId)
            .orElseThrow(() -> Errors.unauthorized("Authentication required"));
    }

    private void checkManagePermission(UUID userId) {
        UserDto user = userApi.findById(userId)
            .orElseThrow(HomeworkErrors::permissionDenied);
        if (user.hasRole(Role.TEACHER) || user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        throw HomeworkErrors.permissionDenied();
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw HomeworkErrors.validationFailed("Title is required");
        }
        if (title.length() > 500) {
            throw HomeworkErrors.validationFailed("Title must not exceed 500 characters");
        }
    }

    private void validatePoints(Integer points) {
        if (points != null && points < 0) {
            throw HomeworkErrors.validationFailed("Points must not be negative");
        }
    }
}
