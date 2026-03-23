package com.example.interhubdev.document.internal.homework;

import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.LessonLookupPort;
import com.example.interhubdev.document.internal.attachment.DocumentAttachmentOwnerType;
import com.example.interhubdev.document.internal.attachment.DocumentAttachmentService;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.fileasset.FileAssetUploadCommand;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link HomeworkApi}: create, list, get, update, delete homework.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class HomeworkServiceImpl implements HomeworkApi {

    private final HomeworkRepository homeworkRepository;
    private final LessonHomeworkRepository lessonHomeworkRepository;
    private final DocumentAttachmentService documentAttachmentService;
    private final LessonLookupPort lessonLookupPort;
    private final UserApi userApi;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public HomeworkDto create(UUID lessonId, String title, String description, Integer points,
                              List<FileAssetUploadCommand> uploads, UUID requesterId) {
        validateRequester(requesterId);
        checkManagePermission(requesterId);

        if (!lessonLookupPort.existsById(lessonId)) {
            throw HomeworkErrors.lessonNotFound(lessonId);
        }
        validateTitle(title);
        validatePoints(points);

        Homework homework = Homework.builder()
            .title(title)
            .description(description)
            .points(points)
            .build();

        try {
            Homework saved = homeworkRepository.save(homework);
            LessonHomework.LessonHomeworkId compositeId = new LessonHomework.LessonHomeworkId(lessonId, saved.getId());
            LessonHomework lessonHomework = new LessonHomework(lessonId, saved.getId(), saved);

            LessonHomework savedLessonHomework = lessonHomeworkRepository.save(lessonHomework);

            entityManager.flush();

            LessonHomework managedInstance = entityManager.find(LessonHomework.class, compositeId);

            saved.setLessonHomework(managedInstance != null ? managedInstance : savedLessonHomework);

            Homework withFiles = homeworkRepository.findByIdWithLessonAndFiles(saved.getId()).orElse(saved);
            return HomeworkMappers.toDto(
                withFiles,
                documentAttachmentService.createAttachments(DocumentAttachmentOwnerType.HOMEWORK, saved.getId(), uploads)
            );
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
        var attachmentsByOwner = documentAttachmentService.findByOwners(
            DocumentAttachmentOwnerType.HOMEWORK,
            list.stream().map(Homework::getId).toList()
        );
        return list.stream()
            .map(homework -> HomeworkMappers.toDto(homework, attachmentsByOwner.getOrDefault(homework.getId(), List.of())))
            .toList();
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
            .map(h -> HomeworkMappers.toDto(
                h,
                documentAttachmentService.findByOwner(DocumentAttachmentOwnerType.HOMEWORK, h.getId())
            ));
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
        var attachmentsByOwner = documentAttachmentService.findByOwners(
            DocumentAttachmentOwnerType.HOMEWORK,
            homeworks.stream().map(Homework::getId).toList()
        );
        return homeworks.stream()
            .map(homework -> HomeworkMappers.toDto(homework, attachmentsByOwner.getOrDefault(homework.getId(), List.of())))
            .toList();
    }

    @Override
    @Transactional
    public HomeworkDto update(UUID homeworkId, String title, String description, Integer points,
                              boolean clearFiles, List<UUID> retainAttachmentIds, List<FileAssetUploadCommand> uploads, UUID requesterId) {
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
        homework.setUpdatedAt(LocalDateTime.now());

        try {
            Homework saved = homeworkRepository.save(homework);
            List<com.example.interhubdev.document.DocumentAttachmentDto> attachments = documentAttachmentService.replaceAttachments(
                DocumentAttachmentOwnerType.HOMEWORK,
                homeworkId,
                retainAttachmentIds,
                clearFiles,
                uploads
            );
            return HomeworkMappers.toDto(saved, attachments);
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
        documentAttachmentService.removeAll(DocumentAttachmentOwnerType.HOMEWORK, homeworkId);
        homeworkRepository.delete(homework);
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
