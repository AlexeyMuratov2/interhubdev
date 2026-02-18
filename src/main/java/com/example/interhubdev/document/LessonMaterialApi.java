package com.example.interhubdev.document;

import com.example.interhubdev.error.AppException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for lesson material operations.
 * Lesson materials are business entities linking a lesson to stored files (one lesson → many materials,
 * one material → many files). Errors are thrown as {@link AppException} and handled by global exception handler.
 */
public interface LessonMaterialApi {

    /**
     * Create a lesson material (optionally with initial files).
     * Requires permission: TEACHER or ADMIN/MODERATOR/SUPER_ADMIN.
     *
     * @param lessonId      lesson UUID (must exist)
     * @param name          material name
     * @param description   optional description
     * @param authorId      user creating the material
     * @param publishedAt   time of publication
     * @param storedFileIds initial list of stored file UUIDs (order preserved as sort_order); may be empty
     * @return created lesson material DTO
     * @throws AppException NOT_FOUND if lesson or any stored file not found, FORBIDDEN if permission denied,
     *                      BAD_REQUEST on validation failure (e.g. duplicate file in list)
     */
    LessonMaterialDto create(UUID lessonId, String name, String description, UUID authorId,
                             LocalDateTime publishedAt, List<UUID> storedFileIds);

    /**
     * List all lesson materials for a lesson, ordered by published_at descending.
     * Requires authentication.
     *
     * @param lessonId    lesson UUID
     * @param requesterId current authenticated user id
     * @return list of lesson material DTOs
     * @throws AppException NOT_FOUND if lesson does not exist, FORBIDDEN if access denied
     */
    List<LessonMaterialDto> listByLesson(UUID lessonId, UUID requesterId);

    /**
     * Get a lesson material by id.
     * Requires authentication.
     *
     * @param materialId  lesson material UUID
     * @param requesterId current authenticated user id
     * @return optional lesson material DTO if found
     * @throws AppException FORBIDDEN if access denied
     */
    Optional<LessonMaterialDto> get(UUID materialId, UUID requesterId);

    /**
     * Delete a lesson material and its file links. Stored files are deleted from storage only if
     * not used elsewhere (e.g. other materials, homework, submissions).
     * Requires permission: material author or ADMIN/MODERATOR/SUPER_ADMIN.
     *
     * @param materialId  lesson material UUID
     * @param requesterId current authenticated user id (for permission check)
     * @throws AppException NOT_FOUND if material not found, FORBIDDEN if permission denied
     */
    void delete(UUID materialId, UUID requesterId);

    /**
     * Add files to an existing lesson material. New files are appended with increasing sort_order.
     * Requires permission: material author or ADMIN/MODERATOR/SUPER_ADMIN.
     *
     * @param materialId     lesson material UUID
     * @param storedFileIds  list of stored file UUIDs to add (order preserved)
     * @param requesterId    current authenticated user id (for permission check)
     * @throws AppException NOT_FOUND if material or any stored file not found, FORBIDDEN if permission denied,
     *                      BAD_REQUEST if file already in material
     */
    void addFiles(UUID materialId, List<UUID> storedFileIds, UUID requesterId);

    /**
     * Remove a file from a lesson material. If the stored file is not used elsewhere, it is deleted from storage.
     * Requires permission: material author or ADMIN/MODERATOR/SUPER_ADMIN.
     *
     * @param materialId    lesson material UUID
     * @param storedFileId  stored file UUID to remove
     * @param requesterId   current authenticated user id (for permission check)
     * @throws AppException NOT_FOUND if material or file link not found, FORBIDDEN if permission denied
     */
    void removeFile(UUID materialId, UUID storedFileId, UUID requesterId);
}
