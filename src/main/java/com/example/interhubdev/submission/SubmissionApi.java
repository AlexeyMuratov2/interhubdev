package com.example.interhubdev.submission;

import com.example.interhubdev.error.AppException;

import java.util.Collection;
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
     * Create or replace a submission for a homework assignment. Only students can submit.
     * At most one submission per student per homework: if the student already has a submission
     * for this homework, it is replaced by the new one (old submission and its files are removed).
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
     * List all submissions for any of the given homework IDs. Single batch query; no N+1.
     * Used by composition to count submitted homeworks per student for a semester.
     *
     * @param homeworkIds homework UUIDs (must not be null; empty returns empty list)
     * @param requesterId  current user (must be TEACHER or ADMIN/MODERATOR/SUPER_ADMIN)
     * @return list of submission DTOs
     * @throws AppException UNAUTHORIZED if not authenticated, FORBIDDEN if not teacher/admin
     */
    List<HomeworkSubmissionDto> listByHomeworkIds(Collection<UUID> homeworkIds, UUID requesterId);

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
     * Get submissions by IDs. Single batch query; no N+1.
     * Used by composition to enrich grade history with submission and homework context.
     *
     * @param submissionIds submission UUIDs (empty returns empty list)
     * @param requesterId   current user (must be TEACHER or ADMIN/MODERATOR/SUPER_ADMIN)
     * @return list of submission DTOs; missing IDs are skipped
     * @throws AppException FORBIDDEN if not teacher/admin
     */
    List<HomeworkSubmissionDto> getByIds(Collection<UUID> submissionIds, UUID requesterId);

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

    /**
     * Check whether the user can download this stored file as a teacher (e.g. when building
     * submissions archive). Used by document module via {@link com.example.interhubdev.document.api.StoredFileDownloadAccessPort}.
     * Returns true if the file is attached to a submission whose homework's lesson is taught by the user.
     *
     * @param storedFileId stored file UUID
     * @param userId       user ID (must be teacher of the lesson for that homework, or admin/moderator is not checked here)
     * @return true if the user is allowed to download this file in teacher context
     */
    boolean canTeacherDownloadSubmissionFile(UUID storedFileId, UUID userId);

    /**
     * Prepare a ZIP archive of all submissions (and their files) for a homework.
     * Only the teacher of the lesson for this homework (or admin/moderator) can call this.
     * Use the returned handle to set Content-Disposition with {@link SubmissionsArchiveHandle#getFilename()}
     * then call {@link SubmissionsArchiveHandle#writeTo(OutputStream)} to stream the ZIP.
     *
     * @param homeworkId   homework UUID
     * @param requesterId  current user (must be teacher of the lesson or admin/moderator)
     * @return handle with suggested filename and writeTo(OutputStream)
     * @throws AppException NOT_FOUND if homework not found, FORBIDDEN if requester has no access
     */
    SubmissionsArchiveHandle buildSubmissionsArchive(UUID homeworkId, UUID requesterId);
}
