package com.example.interhubdev.document;

import com.example.interhubdev.error.AppException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for homework operations.
 * Homework is assigned to a lesson; file reference is optional. Clearing the file reference does not delete the file.
 * Errors are thrown as {@link AppException} and handled by global exception handler.
 */
public interface HomeworkApi {

    /**
     * Create homework for a lesson.
     * Requires TEACHER or ADMIN/MODERATOR/SUPER_ADMIN role.
     *
     * @param lessonId     lesson UUID (must exist)
     * @param title        title (required)
     * @param description  optional description
     * @param points       optional max points
     * @param storedFileId optional stored file UUID (must exist if provided)
     * @param requesterId  current user (for permission check)
     * @return created homework DTO
     * @throws AppException BAD_REQUEST on validation, NOT_FOUND if lesson or file not found,
     *                      FORBIDDEN if permission denied
     */
    HomeworkDto create(UUID lessonId, String title, String description, Integer points,
                       UUID storedFileId, UUID requesterId);

    /**
     * List homeworks for a lesson.
     *
     * @param lessonId    lesson UUID (must exist)
     * @param requesterId current user (for auth)
     * @return list of homework DTOs
     * @throws AppException NOT_FOUND if lesson not found, FORBIDDEN if access denied
     */
    List<HomeworkDto> listByLesson(UUID lessonId, UUID requesterId);

    /**
     * Get homework by id.
     *
     * @param homeworkId   homework UUID
     * @param requesterId  current user (for auth)
     * @return optional homework DTO if found
     * @throws AppException FORBIDDEN if access denied
     */
    Optional<HomeworkDto> get(UUID homeworkId, UUID requesterId);

    /**
     * Update homework. Any field can be changed. If file is cleared, only the reference is removed; the file is not deleted.
     *
     * @param homeworkId   homework UUID
     * @param title        optional new title (null = no change)
     * @param description  optional new description (null = no change)
     * @param points       optional new points (null = no change)
     * @param clearFile    if true, clear the file reference (file in storage is not deleted)
     * @param storedFileId optional new file id (used only if clearFile is false; null = no change)
     * @param requesterId  current user (for permission check)
     * @return updated homework DTO
     * @throws AppException NOT_FOUND if homework or new file not found, FORBIDDEN if permission denied,
     *                      BAD_REQUEST on validation
     */
    HomeworkDto update(UUID homeworkId, String title, String description, Integer points,
                       boolean clearFile, UUID storedFileId, UUID requesterId);

    /**
     * Delete homework. Does not delete the attached file (if any).
     *
     * @param homeworkId   homework UUID
     * @param requesterId  current user (for permission check)
     * @throws AppException NOT_FOUND if homework not found, FORBIDDEN if permission denied
     */
    void delete(UUID homeworkId, UUID requesterId);
}
