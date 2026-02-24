package com.example.interhubdev.document;

import com.example.interhubdev.error.AppException;

import java.util.Collection;
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
     * @param lessonId      lesson UUID (must exist)
     * @param title         title (required)
     * @param description   optional description
     * @param points        optional max points
     * @param storedFileIds optional list of stored file UUIDs (must exist if provided); order preserved
     * @param requesterId   current user (for permission check)
     * @return created homework DTO
     * @throws AppException BAD_REQUEST on validation, NOT_FOUND if lesson or any file not found,
     *                      FORBIDDEN if permission denied
     */
    HomeworkDto create(UUID lessonId, String title, String description, Integer points,
                       List<UUID> storedFileIds, UUID requesterId);

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
     * List all homework IDs linked to any of the given lessons. Single batch query; no N+1.
     * Used by composition to get total homework count and submission counts per student for a semester.
     *
     * @param lessonIds   lesson UUIDs (must not be null; empty returns empty list)
     * @param requesterId current user (must be authenticated)
     * @return list of homework IDs (distinct)
     * @throws AppException UNAUTHORIZED if requester not authenticated
     */
    List<UUID> listHomeworkIdsByLessonIds(Collection<UUID> lessonIds, UUID requesterId);

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
     * Update homework. Any field can be changed. If files are cleared, only the links are removed; files are not deleted.
     *
     * @param homeworkId    homework UUID
     * @param title         optional new title (null = no change)
     * @param description   optional new description (null = no change)
     * @param points        optional new points (null = no change)
     * @param clearFiles    if true, clear all file links (files in storage are not deleted)
     * @param storedFileIds optional new list of file ids (used only if clearFiles is false; full replacement, order preserved)
     * @param requesterId   current user (for permission check)
     * @return updated homework DTO
     * @throws AppException NOT_FOUND if homework or any file not found, FORBIDDEN if permission denied,
     *                      BAD_REQUEST on validation
     */
    HomeworkDto update(UUID homeworkId, String title, String description, Integer points,
                       boolean clearFiles, List<UUID> storedFileIds, UUID requesterId);

    /**
     * Delete homework. Does not delete the attached file (if any).
     *
     * @param homeworkId   homework UUID
     * @param requesterId  current user (for permission check)
     * @throws AppException NOT_FOUND if homework not found, FORBIDDEN if permission denied
     */
    void delete(UUID homeworkId, UUID requesterId);
}
