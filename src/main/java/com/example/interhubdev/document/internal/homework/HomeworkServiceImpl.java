package com.example.interhubdev.document.internal.homework;

import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.LessonLookupPort;
import com.example.interhubdev.document.internal.storedFile.DocumentErrors;
import com.example.interhubdev.storedfile.StoredFileApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link HomeworkApi}: create, list, get, update, delete homework.
 * When file links are cleared we only remove the links; we do not delete the stored files.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class HomeworkServiceImpl implements HomeworkApi {

    private final HomeworkRepository homeworkRepository;
    private final HomeworkFileRepository homeworkFileRepository;
    private final LessonHomeworkRepository lessonHomeworkRepository;
    private final StoredFileApi storedFileApi;
    private final DocumentApi documentApi;
    private final LessonLookupPort lessonLookupPort;
    private final UserApi userApi;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public HomeworkDto create(UUID lessonId, String title, String description, Integer points,
                              List<UUID> storedFileIds, UUID requesterId) {
        validateRequester(requesterId);
        checkManagePermission(requesterId);

        if (!lessonLookupPort.existsById(lessonId)) {
            throw HomeworkErrors.lessonNotFound(lessonId);
        }
        validateTitle(title);
        validatePoints(points);

        List<UUID> fileIds = storedFileIds != null ? storedFileIds : List.of();
        if (fileIds.stream().distinct().count() != fileIds.size()) {
            throw HomeworkErrors.validationFailed("Duplicate file IDs in request");
        }

        Homework homework = Homework.builder()
            .title(title)
            .description(description)
            .points(points)
            .build();

        try {
            Homework saved = homeworkRepository.save(homework);

            for (int i = 0; i < fileIds.size(); i++) {
                UUID fileId = fileIds.get(i);
                com.example.interhubdev.document.StoredFileDto fileDto = documentApi.getStoredFile(fileId)
                    .orElseThrow(() -> DocumentErrors.storedFileNotFound(fileId));
                if (!fileDto.isActive()) {
                    throw DocumentErrors.fileNotActiveForBind();
                }
                HomeworkFile hf = new HomeworkFile(saved.getId(), fileId, i, saved);
                homeworkFileRepository.save(hf);
            }

            LessonHomework.LessonHomeworkId compositeId = new LessonHomework.LessonHomeworkId(lessonId, saved.getId());
            LessonHomework lessonHomework = new LessonHomework(lessonId, saved.getId(), saved);

            LessonHomework savedLessonHomework = lessonHomeworkRepository.save(lessonHomework);

            entityManager.flush();

            LessonHomework managedInstance = entityManager.find(LessonHomework.class, compositeId);

            saved.setLessonHomework(managedInstance != null ? managedInstance : savedLessonHomework);

            Homework withFiles = homeworkRepository.findByIdWithLessonAndFiles(saved.getId()).orElse(saved);
            Map<UUID, com.example.interhubdev.document.StoredFileDto> filesMap = documentApi.getStoredFiles(
                withFiles.getFiles().stream().map(HomeworkFile::getStoredFileId).collect(Collectors.toSet()));
            return HomeworkMappers.toDto(withFiles, filesMap);
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
        List<Homework> list = homeworkRepository.findByLessonIdOrderByCreatedAtDescWithFiles(lessonId);
        Set<UUID> allFileIds = list.stream()
            .flatMap(h -> h.getFiles().stream().map(HomeworkFile::getStoredFileId))
            .collect(Collectors.toSet());
        Map<UUID, com.example.interhubdev.document.StoredFileDto> filesMap = documentApi.getStoredFiles(allFileIds);
        return list.stream().map(h -> HomeworkMappers.toDto(h, filesMap)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listHomeworkIdsByLessonIds(Collection<UUID> lessonIds, UUID requesterId) {
        validateRequester(requesterId);
        if (lessonIds == null || lessonIds.isEmpty()) {
            return List.of();
        }
        return lessonHomeworkRepository.findHomeworkIdsByLessonIdIn(lessonIds).stream().distinct().toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HomeworkDto> get(UUID homeworkId, UUID requesterId) {
        validateRequester(requesterId);
        return homeworkRepository.findByIdWithLessonAndFiles(homeworkId)
            .map(h -> {
                Map<UUID, com.example.interhubdev.document.StoredFileDto> filesMap = documentApi.getStoredFiles(
                    h.getFiles().stream().map(HomeworkFile::getStoredFileId).collect(Collectors.toSet()));
                return HomeworkMappers.toDto(h, filesMap);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkDto> getByIds(Collection<UUID> homeworkIds, UUID requesterId) {
        validateRequester(requesterId);
        if (homeworkIds == null || homeworkIds.isEmpty()) {
            return List.of();
        }
        List<Homework> homeworks = homeworkIds.stream()
                .map(id -> homeworkRepository.findByIdWithLessonAndFiles(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        Set<UUID> allFileIds = homeworks.stream()
            .flatMap(h -> h.getFiles().stream().map(HomeworkFile::getStoredFileId))
            .collect(Collectors.toSet());
        Map<UUID, com.example.interhubdev.document.StoredFileDto> filesMap = documentApi.getStoredFiles(allFileIds);
        return homeworks.stream().map(h -> HomeworkMappers.toDto(h, filesMap)).toList();
    }

    @Override
    @Transactional
    public HomeworkDto update(UUID homeworkId, String title, String description, Integer points,
                              boolean clearFiles, List<UUID> storedFileIds, UUID requesterId) {
        validateRequester(requesterId);
        checkManagePermission(requesterId);

        Homework homework = homeworkRepository.findByIdWithLessonAndFiles(homeworkId)
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

        if (clearFiles) {
            homework.getFiles().clear();
        } else if (storedFileIds != null) {
            if (storedFileIds.stream().distinct().count() != storedFileIds.size()) {
                throw HomeworkErrors.validationFailed("Duplicate file IDs in request");
            }
            for (UUID fileId : storedFileIds) {
                com.example.interhubdev.document.StoredFileDto fileDto = documentApi.getStoredFile(fileId)
                    .orElseThrow(() -> DocumentErrors.storedFileNotFound(fileId));
                if (!fileDto.isActive()) {
                    throw DocumentErrors.fileNotActiveForBind();
                }
            }
            homework.getFiles().clear();
            for (int i = 0; i < storedFileIds.size(); i++) {
                UUID fileId = storedFileIds.get(i);
                HomeworkFile hf = new HomeworkFile(homework.getId(), fileId, i, homework);
                homework.getFiles().add(hf);
            }
        }
        homework.setUpdatedAt(LocalDateTime.now());

        try {
            Homework saved = homeworkRepository.save(homework);
            Map<UUID, com.example.interhubdev.document.StoredFileDto> filesMap = documentApi.getStoredFiles(
                saved.getFiles().stream().map(HomeworkFile::getStoredFileId).collect(Collectors.toSet()));
            return HomeworkMappers.toDto(saved, filesMap);
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
        // Junction table entry is deleted via CASCADE in database
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
