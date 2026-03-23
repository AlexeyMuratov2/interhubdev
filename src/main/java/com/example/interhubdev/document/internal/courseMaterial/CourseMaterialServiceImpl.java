package com.example.interhubdev.document.internal.courseMaterial;

import com.example.interhubdev.document.CourseMaterialApi;
import com.example.interhubdev.document.CourseMaterialDto;
import com.example.interhubdev.document.OfferingLookupPort;
import com.example.interhubdev.document.internal.attachment.DocumentAttachmentOwnerType;
import com.example.interhubdev.document.internal.attachment.DocumentAttachmentService;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.fileasset.FileAssetUploadCommand;
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
 * Implementation of {@link CourseMaterialApi}: create, list, get, delete course materials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class CourseMaterialServiceImpl implements CourseMaterialApi {

    private final CourseMaterialRepository courseMaterialRepository;
    private final DocumentAttachmentService documentAttachmentService;
    private final UserApi userApi;
    private final OfferingLookupPort offeringLookupPort;

    @Override
    @Transactional
    public CourseMaterialDto createMaterial(UUID offeringId, FileAssetUploadCommand upload, String title, String description, UUID authorId) {
        validateTitle(title);
        checkCreatePermission(authorId);
        if (!offeringLookupPort.existsById(offeringId)) {
            throw CourseMaterialErrors.offeringNotFound(offeringId);
        }

        CourseMaterial material = CourseMaterial.builder()
            .offeringId(offeringId)
            .title(title)
            .description(description)
            .authorId(authorId)
            .build();

        try {
            CourseMaterial saved = courseMaterialRepository.save(material);
            var attachments = documentAttachmentService.createAttachments(
                DocumentAttachmentOwnerType.COURSE_MATERIAL,
                saved.getId(),
                List.of(upload)
            );
            return CourseMaterialMappers.toDto(saved, attachments.isEmpty() ? null : attachments.get(0));
        } catch (PersistenceException | DataIntegrityViolationException e) {
            log.warn("Failed to save course material (offeringId={}): {}", offeringId, e.getMessage());
            throw CourseMaterialErrors.saveFailed();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseMaterialDto> listByOffering(UUID offeringId, UUID requesterId) {
        userApi.findById(requesterId)
            .orElseThrow(() -> Errors.unauthorized("Authentication required"));
        if (!offeringLookupPort.existsById(offeringId)) {
            throw CourseMaterialErrors.offeringNotFound(offeringId);
        }

        List<CourseMaterial> materials = courseMaterialRepository.findByOfferingIdOrderByUploadedAtDesc(offeringId);
        var attachmentsByOwner = documentAttachmentService.findByOwners(
            DocumentAttachmentOwnerType.COURSE_MATERIAL,
            materials.stream().map(CourseMaterial::getId).toList()
        );
        return materials.stream()
            .map(material -> CourseMaterialMappers.toDto(
                material,
                attachmentsByOwner.getOrDefault(material.getId(), List.of()).stream().findFirst().orElse(null)
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseMaterialDto> get(UUID materialId, UUID requesterId) {
        userApi.findById(requesterId)
            .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return courseMaterialRepository.findById(materialId)
            .map(material -> CourseMaterialMappers.toDto(
                material,
                documentAttachmentService.findByOwner(DocumentAttachmentOwnerType.COURSE_MATERIAL, material.getId())
                    .stream()
                    .findFirst()
                    .orElse(null)
            ));
    }

    @Override
    @Transactional
    public void delete(UUID materialId, UUID requesterId) {
        CourseMaterial material = courseMaterialRepository.findById(materialId)
            .orElseThrow(() -> CourseMaterialErrors.materialNotFound(materialId));
        checkDeletePermission(material, requesterId);
        documentAttachmentService.removeAll(DocumentAttachmentOwnerType.COURSE_MATERIAL, materialId);
        courseMaterialRepository.delete(material);
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
