package com.example.interhubdev.document.internal.lessonMaterial;

import com.example.interhubdev.document.LessonMaterialApi;
import com.example.interhubdev.document.LessonMaterialDto;
import com.example.interhubdev.document.LessonLookupPort;
import com.example.interhubdev.document.internal.attachment.DocumentAttachmentOwnerType;
import com.example.interhubdev.document.internal.attachment.DocumentAttachmentService;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.fileasset.FileAssetUploadCommand;
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
 * Implementation of {@link LessonMaterialApi}: create, list, get, delete lesson materials; add/remove files.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class LessonMaterialServiceImpl implements LessonMaterialApi {

    private static final int MAX_NAME_LENGTH = 500;

    private final LessonMaterialRepository lessonMaterialRepository;
    private final DocumentAttachmentService documentAttachmentService;
    private final LessonLookupPort lessonLookupPort;
    private final UserApi userApi;

    @Override
    @Transactional
    public LessonMaterialDto create(UUID lessonId, String name, String description, UUID authorId,
                                    LocalDateTime publishedAt, List<FileAssetUploadCommand> uploads) {
        validateName(name);
        checkCreatePermission(authorId);

        if (!lessonLookupPort.existsById(lessonId)) {
            throw LessonMaterialErrors.lessonNotFound(lessonId);
        }

        LessonMaterial material = LessonMaterial.builder()
            .lessonId(lessonId)
            .name(name.trim())
            .description(description != null ? description.trim() : null)
            .authorId(authorId)
            .publishedAt(publishedAt != null ? publishedAt : LocalDateTime.now())
            .build();

        try {
            LessonMaterial saved = lessonMaterialRepository.save(material);
            LessonMaterial withFiles = lessonMaterialRepository.findByIdWithFiles(saved.getId()).orElseThrow();
            return LessonMaterialMappers.toDto(
                withFiles,
                documentAttachmentService.createAttachments(DocumentAttachmentOwnerType.LESSON_MATERIAL, saved.getId(), uploads)
            );
        } catch (PersistenceException | DataIntegrityViolationException e) {
            log.warn("Failed to save lesson material (lessonId={}): {}", lessonId, e.getMessage());
            throw LessonMaterialErrors.saveFailed();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonMaterialDto> listByLesson(UUID lessonId, UUID requesterId) {
        userApi.findById(requesterId)
            .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        if (!lessonLookupPort.existsById(lessonId)) {
            throw LessonMaterialErrors.lessonNotFound(lessonId);
        }

        List<LessonMaterial> materials = lessonMaterialRepository.findByLessonIdOrderByPublishedAtDescWithFiles(lessonId);
        var attachmentsByOwner = documentAttachmentService.findByOwners(
            DocumentAttachmentOwnerType.LESSON_MATERIAL,
            materials.stream().map(LessonMaterial::getId).toList()
        );
        return materials.stream()
            .map(material -> LessonMaterialMappers.toDto(material, attachmentsByOwner.getOrDefault(material.getId(), List.of())))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LessonMaterialDto> get(UUID materialId, UUID requesterId) {
        userApi.findById(requesterId)
            .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return lessonMaterialRepository.findByIdWithFiles(materialId)
            .map(material -> LessonMaterialMappers.toDto(
                material,
                documentAttachmentService.findByOwner(DocumentAttachmentOwnerType.LESSON_MATERIAL, material.getId())
            ));
    }

    @Override
    @Transactional
    public void delete(UUID materialId, UUID requesterId) {
        LessonMaterial material = lessonMaterialRepository.findByIdWithFiles(materialId)
            .orElseThrow(() -> LessonMaterialErrors.materialNotFound(materialId));

        checkModifyPermission(material, requesterId);
        documentAttachmentService.removeAll(DocumentAttachmentOwnerType.LESSON_MATERIAL, materialId);
        lessonMaterialRepository.delete(material);
    }

    @Override
    @Transactional
    public void addFiles(UUID materialId, List<FileAssetUploadCommand> uploads, UUID requesterId) {
        if (uploads == null || uploads.isEmpty()) {
            return;
        }

        LessonMaterial material = lessonMaterialRepository.findByIdWithFiles(materialId)
            .orElseThrow(() -> LessonMaterialErrors.materialNotFound(materialId));

        checkModifyPermission(material, requesterId);
        documentAttachmentService.createAttachments(DocumentAttachmentOwnerType.LESSON_MATERIAL, materialId, uploads);
    }

    @Override
    @Transactional
    public void removeFile(UUID materialId, UUID attachmentId, UUID requesterId) {
        LessonMaterial material = lessonMaterialRepository.findById(materialId)
            .orElseThrow(() -> LessonMaterialErrors.materialNotFound(materialId));

        checkModifyPermission(material, requesterId);
        documentAttachmentService.removeAttachment(DocumentAttachmentOwnerType.LESSON_MATERIAL, materialId, attachmentId);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw LessonMaterialErrors.invalidName("Name is required");
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw LessonMaterialErrors.invalidName("Name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
    }

    private void checkCreatePermission(UUID userId) {
        UserDto user = userApi.findById(userId)
            .orElseThrow(() -> LessonMaterialErrors.createPermissionDenied());
        if (user.hasRole(Role.TEACHER) || user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        throw LessonMaterialErrors.createPermissionDenied();
    }

    private void checkModifyPermission(LessonMaterial material, UUID requesterId) {
        if (material.getAuthorId().equals(requesterId)) {
            return;
        }
        UserDto user = userApi.findById(requesterId)
            .orElseThrow(() -> LessonMaterialErrors.permissionDenied());
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        throw LessonMaterialErrors.permissionDenied();
    }
}
