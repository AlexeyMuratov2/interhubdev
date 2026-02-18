package com.example.interhubdev.document.internal.lessonMaterial;

import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.LessonMaterialApi;
import com.example.interhubdev.document.LessonMaterialDto;
import com.example.interhubdev.document.LessonLookupPort;
import com.example.interhubdev.document.internal.storedFile.DocumentErrors;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link LessonMaterialApi}: create, list, get, delete lesson materials; add/remove files.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class LessonMaterialServiceImpl implements LessonMaterialApi {

    private static final int MAX_NAME_LENGTH = 500;

    private final LessonMaterialRepository lessonMaterialRepository;
    private final LessonMaterialFileRepository lessonMaterialFileRepository;
    private final StoredFileRepository storedFileRepository;
    private final DocumentApi documentApi;
    private final LessonLookupPort lessonLookupPort;
    private final UserApi userApi;

    @Override
    @Transactional
    public LessonMaterialDto create(UUID lessonId, String name, String description, UUID authorId,
                                    LocalDateTime publishedAt, List<UUID> storedFileIds) {
        validateName(name);
        checkCreatePermission(authorId);

        if (!lessonLookupPort.existsById(lessonId)) {
            throw LessonMaterialErrors.lessonNotFound(lessonId);
        }

        List<UUID> fileIds = storedFileIds != null ? storedFileIds : List.of();
        if (fileIds.stream().distinct().count() != fileIds.size()) {
            throw LessonMaterialErrors.invalidName("Duplicate file IDs in request");
        }

        List<StoredFile> storedFiles = new ArrayList<>();
        for (UUID fileId : fileIds) {
            StoredFile file = storedFileRepository.findById(fileId)
                .orElseThrow(() -> DocumentErrors.storedFileNotFound(fileId));
            storedFiles.add(file);
        }

        LessonMaterial material = LessonMaterial.builder()
            .lessonId(lessonId)
            .name(name.trim())
            .description(description != null ? description.trim() : null)
            .authorId(authorId)
            .publishedAt(publishedAt != null ? publishedAt : LocalDateTime.now())
            .files(new ArrayList<>())
            .build();

        try {
            LessonMaterial saved = lessonMaterialRepository.save(material);
            int sortOrder = 0;
            for (StoredFile file : storedFiles) {
                LessonMaterialFile lmf = new LessonMaterialFile(
                    saved.getId(), file.getId(), sortOrder++, null, file
                );
                lessonMaterialFileRepository.save(lmf);
            }
            return LessonMaterialMappers.toDto(
                lessonMaterialRepository.findByIdWithFiles(saved.getId()).orElseThrow()
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
        return materials.stream().map(LessonMaterialMappers::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LessonMaterialDto> get(UUID materialId, UUID requesterId) {
        userApi.findById(requesterId)
            .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return lessonMaterialRepository.findByIdWithFiles(materialId).map(LessonMaterialMappers::toDto);
    }

    @Override
    @Transactional
    public void delete(UUID materialId, UUID requesterId) {
        LessonMaterial material = lessonMaterialRepository.findByIdWithFiles(materialId)
            .orElseThrow(() -> LessonMaterialErrors.materialNotFound(materialId));

        checkModifyPermission(material, requesterId);

        List<UUID> storedFileIds = material.getFiles().stream()
            .map(LessonMaterialFile::getStoredFileId)
            .toList();

        lessonMaterialRepository.delete(material);

        for (UUID storedFileId : storedFileIds) {
            try {
                documentApi.deleteStoredFile(storedFileId, requesterId);
            } catch (Exception e) {
                log.warn("Failed to delete stored file after lesson material deletion: {}", storedFileId, e);
            }
        }
    }

    @Override
    @Transactional
    public void addFiles(UUID materialId, List<UUID> storedFileIds, UUID requesterId) {
        if (storedFileIds == null || storedFileIds.isEmpty()) {
            return;
        }

        LessonMaterial material = lessonMaterialRepository.findByIdWithFiles(materialId)
            .orElseThrow(() -> LessonMaterialErrors.materialNotFound(materialId));

        checkModifyPermission(material, requesterId);

        Set<UUID> existingFileIds = material.getFiles().stream()
            .map(LessonMaterialFile::getStoredFileId)
            .collect(Collectors.toSet());

        List<UUID> toAdd = storedFileIds.stream().distinct().toList();
        for (UUID fileId : toAdd) {
            if (existingFileIds.contains(fileId)) {
                throw LessonMaterialErrors.fileAlreadyInMaterial(fileId);
            }
        }

        int nextOrder = lessonMaterialFileRepository.findMaxSortOrderByLessonMaterialId(materialId) + 1;
        for (UUID fileId : toAdd) {
            StoredFile file = storedFileRepository.findById(fileId)
                .orElseThrow(() -> LessonMaterialErrors.storedFileNotFound(fileId));
            LessonMaterialFile lmf = new LessonMaterialFile(
                materialId, file.getId(), nextOrder++, null, file
            );
            lessonMaterialFileRepository.save(lmf);
        }
    }

    @Override
    @Transactional
    public void removeFile(UUID materialId, UUID storedFileId, UUID requesterId) {
        LessonMaterial material = lessonMaterialRepository.findById(materialId)
            .orElseThrow(() -> LessonMaterialErrors.materialNotFound(materialId));

        checkModifyPermission(material, requesterId);

        LessonMaterialFile link = lessonMaterialFileRepository.findByLessonMaterialIdAndStoredFileId(materialId, storedFileId)
            .orElseThrow(() -> LessonMaterialErrors.fileLinkNotFound(materialId, storedFileId));

        lessonMaterialFileRepository.delete(link);

        try {
            documentApi.deleteStoredFile(storedFileId, requesterId);
        } catch (Exception e) {
            log.warn("Failed to delete stored file after removing from lesson material: {}", storedFileId, e);
        }
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
