package com.example.interhubdev.document.internal.courseMaterial;

import com.example.interhubdev.document.CourseMaterialApi;
import com.example.interhubdev.document.CourseMaterialDto;
import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.internal.storedFile.DocumentErrors;
import com.example.interhubdev.document.internal.storedFile.StoredFile;
import com.example.interhubdev.document.internal.storedFile.StoredFileRepository;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.user.Role;
import jakarta.persistence.PersistenceException;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link CourseMaterialApi}: create (attach stored file), list, get, delete course materials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class CourseMaterialServiceImpl implements CourseMaterialApi {

    private final CourseMaterialRepository courseMaterialRepository;
    private final StoredFileRepository storedFileRepository;
    private final DocumentApi documentApi;
    private final UserApi userApi;

    @Override
    @Transactional
    public CourseMaterialDto createMaterial(UUID subjectId, UUID storedFileId, String title, String description, UUID authorId) {
        // Validate title
        validateTitle(title);

        // Check permission: TEACHER or ADMIN
        checkCreatePermission(authorId);

        // Get stored file
        StoredFile storedFile = storedFileRepository.findById(storedFileId)
            .orElseThrow(() -> DocumentErrors.storedFileNotFound(storedFileId));

        // Check if material with same subject+file already exists
        courseMaterialRepository.findBySubjectIdOrderByUploadedAtDesc(subjectId).stream()
            .filter(m -> m.getStoredFile().getId().equals(storedFileId))
            .findFirst()
            .ifPresent(m -> {
                throw CourseMaterialErrors.materialAlreadyExists(subjectId);
            });

        // Create course material
        CourseMaterial material = CourseMaterial.builder()
            .subjectId(subjectId)
            .storedFile(storedFile)
            .title(title)
            .description(description)
            .authorId(authorId)
            .build();

        try {
            CourseMaterial saved = courseMaterialRepository.save(material);
            return CourseMaterialMappers.toDto(saved);
        } catch (PersistenceException | DataIntegrityViolationException e) {
            log.warn("Failed to save course material (subjectId={}, storedFileId={}): {}", subjectId, storedFileId, e.getMessage());
            throw CourseMaterialErrors.saveFailed();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseMaterialDto> listBySubject(UUID subjectId, UUID requesterId) {
        // Check authentication (requesterId must be valid user)
        userApi.findById(requesterId)
            .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        List<CourseMaterial> materials = courseMaterialRepository.findBySubjectIdOrderByUploadedAtDesc(subjectId);
        return materials.stream()
            .map(CourseMaterialMappers::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseMaterialDto> get(UUID materialId, UUID requesterId) {
        // Check authentication (requesterId must be valid user)
        userApi.findById(requesterId)
            .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return courseMaterialRepository.findById(materialId)
            .map(CourseMaterialMappers::toDto);
    }

    @Override
    @Transactional
    public void delete(UUID materialId, UUID requesterId) {
        CourseMaterial material = courseMaterialRepository.findById(materialId)
            .orElseThrow(() -> CourseMaterialErrors.materialNotFound(materialId));

        // Check permission: author or ADMIN/MODERATOR
        checkDeletePermission(material, requesterId);

        UUID storedFileId = material.getStoredFile().getId();

        // Delete course material
        courseMaterialRepository.delete(material);

        // Check if stored file is still used by other materials
        long usageCount = courseMaterialRepository.countByStoredFileId(storedFileId);
        if (usageCount == 0) {
            // No other materials use this file, safe to delete
            try {
                documentApi.deleteStoredFile(storedFileId, requesterId);
            } catch (Exception e) {
                log.warn("Failed to delete stored file after material deletion: {}", storedFileId, e);
                // Don't throw - material is already deleted, file cleanup can happen later
            }
        }
    }

    /**
     * Validate material title.
     */
    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw CourseMaterialErrors.invalidTitle("Title is required");
        }
        if (title.length() > 500) {
            throw CourseMaterialErrors.invalidTitle("Title must not exceed 500 characters");
        }
    }

    /**
     * Check if user has permission to create course materials.
     * Rule: user must have TEACHER or ADMIN/MODERATOR/SUPER_ADMIN role.
     */
    private void checkCreatePermission(UUID userId) {
        UserDto user = userApi.findById(userId)
            .orElseThrow(() -> CourseMaterialErrors.createPermissionDenied());
        if (user.hasRole(Role.TEACHER) || user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        throw CourseMaterialErrors.createPermissionDenied();
    }

    /**
     * Check if user has permission to delete a course material.
     * Rule: material author OR user with ADMIN/MODERATOR/SUPER_ADMIN role.
     */
    private void checkDeletePermission(CourseMaterial material, UUID requesterId) {
        if (material.getAuthorId().equals(requesterId)) {
            return; // Author can delete
        }
        UserDto user = userApi.findById(requesterId)
            .orElseThrow(() -> CourseMaterialErrors.deletePermissionDenied());
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return; // Admin/Moderator can delete
        }
        throw CourseMaterialErrors.deletePermissionDenied();
    }
}
