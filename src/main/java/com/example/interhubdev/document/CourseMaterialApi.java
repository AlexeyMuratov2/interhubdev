package com.example.interhubdev.document;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.fileasset.FileAssetUploadCommand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for course material operations.
 * Course materials are business entities linking group_subject_offering to stored files.
 * Materials belong to a specific offering (group + curriculum_subject + teacher), allowing each teacher
 * to have their own materials for the same subject.
 * Errors are thrown as {@link AppException} and handled by global exception handler.
 */
public interface CourseMaterialApi {

    /**
     * Create a course material with a single uploaded attachment.
     * Requires permission: TEACHER or ADMIN role.
     *
     * @param offeringId  group subject offering UUID (must exist)
     * @param upload      uploaded file
     * @param title       material title
     * @param description optional description
     * @param authorId    user creating the material (must match current user or be ADMIN)
     * @return created course material DTO
     * @throws AppException NOT_FOUND if offering not found, FORBIDDEN if permission denied,
     *                      BAD_REQUEST on validation failure
     */
    CourseMaterialDto createMaterial(UUID offeringId, FileAssetUploadCommand upload, String title, String description, UUID authorId);

    /**
     * List all course materials for an offering.
     * Requires authentication.
     *
     * @param offeringId  group subject offering UUID
     * @param requesterId current authenticated user id (for access control)
     * @return list of course materials, ordered by uploaded_at descending
     * @throws AppException NOT_FOUND if offering not found, FORBIDDEN if access denied
     */
    List<CourseMaterialDto> listByOffering(UUID offeringId, UUID requesterId);

    /**
     * Get a course material by id.
     * Requires authentication.
     *
     * @param materialId  course material UUID
     * @param requesterId current authenticated user id (for access control)
     * @return optional course material DTO if found
     * @throws AppException NOT_FOUND if material not found, FORBIDDEN if access denied
     */
    Optional<CourseMaterialDto> get(UUID materialId, UUID requesterId);

    /**
     * Delete a course material.
     * If the stored file is not used by any other material, it will be deleted from storage.
     * Requires permission: material author or ADMIN/MODERATOR role.
     *
     * @param materialId  course material UUID
     * @param requesterId current authenticated user id (for permission check)
     * @throws AppException NOT_FOUND if material not found, FORBIDDEN if permission denied
     */
    void delete(UUID materialId, UUID requesterId);
}
