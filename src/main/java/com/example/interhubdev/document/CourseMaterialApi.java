package com.example.interhubdev.document;

import com.example.interhubdev.error.AppException;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for course material operations.
 * Course materials are business entities linking subjects to stored files.
 * Errors are thrown as {@link AppException} and handled by global exception handler.
 */
public interface CourseMaterialApi {

    /**
     * Create a course material from an existing stored file.
     * Requires permission: TEACHER or ADMIN role.
     *
     * @param subjectId   subject UUID
     * @param storedFileId existing stored file UUID
     * @param title       material title
     * @param description optional description
     * @param authorId    user creating the material (must match current user or be ADMIN)
     * @return created course material DTO
     * @throws AppException NOT_FOUND if stored file not found, FORBIDDEN if permission denied,
     *                      BAD_REQUEST on validation failure
     */
    CourseMaterialDto createMaterial(UUID subjectId, UUID storedFileId, String title, String description, UUID authorId);

    /**
     * Upload a file and create a course material in one operation (convenience method).
     * Atomic: on DB failure after S3 upload, the file is removed from S3.
     * Requires permission: TEACHER or ADMIN role.
     *
     * @param subjectId      subject UUID
     * @param fileStream     file content stream
     * @param originalName   original file name
     * @param contentType    MIME type
     * @param size           file size in bytes
     * @param title          material title
     * @param description    optional description
     * @param authorId       user uploading the material (must match current user or be ADMIN)
     * @return created course material DTO
     * @throws AppException BAD_REQUEST on validation failure, FORBIDDEN if permission denied,
     *                      INTERNAL_ERROR if upload fails
     */
    CourseMaterialDto uploadMaterial(
        UUID subjectId,
        InputStream fileStream,
        String originalName,
        String contentType,
        long size,
        String title,
        String description,
        UUID authorId
    );

    /**
     * List all course materials for a subject.
     * Requires authentication.
     *
     * @param subjectId   subject UUID
     * @param requesterId current authenticated user id (for access control)
     * @return list of course materials, ordered by uploaded_at descending
     * @throws AppException FORBIDDEN if access denied
     */
    List<CourseMaterialDto> listBySubject(UUID subjectId, UUID requesterId);

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
