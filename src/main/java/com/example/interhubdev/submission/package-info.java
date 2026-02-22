/**
 * Submission module - student solutions for homework assignments.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.submission.SubmissionApi} - create, list, get, delete submissions, build submissions archive</li>
 *   <li>{@link com.example.interhubdev.submission.HomeworkSubmissionDto} - submission DTO</li>
 *   <li>{@link com.example.interhubdev.submission.SubmissionsArchiveHandle} - handle for streaming submissions ZIP by homework</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * <ul>
 *   <li>Create (submit): only STUDENT role</li>
 *   <li>List by homework / Get: only TEACHER or ADMIN/MODERATOR/SUPER_ADMIN</li>
 *   <li>Delete: only the author (student who submitted)</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>document - HomeworkApi (validate homework exists), DocumentApi (stored files, download), StoredFileDto</li>
 *   <li>auth - current user</li>
 *   <li>user - roles for permission checks</li>
 *   <li>error - AppException, Errors</li>
 *   <li>schedule - lesson by id (teacher-of-lesson check, archive metadata)</li>
 *   <li>offering - offering and offering teachers (teacher-of-lesson check)</li>
 *   <li>teacher - teacher by user id (teacher-of-lesson check)</li>
 *   <li>program - curriculum subject (for subject id from offering)</li>
 *   <li>subject - subject name for archive filename</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Submission",
    allowedDependencies = {"document", "auth", "user", "error", "schedule", "offering", "teacher", "program", "subject"}
)
package com.example.interhubdev.submission;
