package com.example.interhubdev.submission;

import com.example.interhubdev.error.AppException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for homework submissions (student solutions).
 * Create is allowed only for students; list/get for teachers and admins; delete only for the author.
 * Errors are thrown as {@link AppException} and handled by global exception handler.
 */
public interface SubmissionApi {

    /**
     * Create a submission for a homework assignment. Only students can submit.
     * Files are optional (can pass empty list; description-only submission).
     *
     * @param homeworkId    homework (assignment) UUID; must exist
     * @param description  optional text description
     * @param storedFileIds optional list of already-uploaded stored file IDs (each must exist)
     * @param requesterId   current user (must have STUDENT role; becomes author)
     * @return created submission DTO
     * @throws AppException NOT_FOUND if homework or any file not found, FORBIDDEN if not student,
     *                      BAD_REQUEST on validation (e.g. description too long)
     */
    HomeworkSubmissionDto create(UUID homeworkId, String description, List<UUID> storedFileIds, UUID requesterId);

    /**
     * List all submissions for a homework. Only teachers and admins can list.
     *
     * @param homeworkId   homework UUID (must exist)
     * @param requesterId  current user (must be TEACHER or ADMIN/MODERATOR/SUPER_ADMIN)
     * @return list of submission DTOs
     * @throws AppException NOT_FOUND if homework not found, FORBIDDEN if not teacher/admin
     */
    List<HomeworkSubmissionDto> listByHomework(UUID homeworkId, UUID requesterId);

    /**
     * Get one submission by id. Only teachers and admins can view.
     *
     * @param submissionId submission UUID
     * @param requesterId   current user (must be TEACHER or ADMIN/MODERATOR/SUPER_ADMIN)
     * @return optional submission DTO if found
     * @throws AppException FORBIDDEN if not teacher/admin
     */
    Optional<HomeworkSubmissionDto> get(UUID submissionId, UUID requesterId);

    /**
     * Delete a submission. Only the author (student who submitted) can delete.
     * Attached files in storage are not deleted.
     *
     * @param submissionId submission UUID
     * @param requesterId  current user (must be the author)
     * @throws AppException NOT_FOUND if submission not found, FORBIDDEN if not the author
     */
    void delete(UUID submissionId, UUID requesterId);

    /**
     * Check if the given stored file is referenced by any submission.
     * Used by document module (via adapter) to prevent deleting files in use.
     *
     * @param storedFileId stored file UUID
     * @return true if at least one submission references this file
     */
    boolean isStoredFileInUse(UUID storedFileId);
}
