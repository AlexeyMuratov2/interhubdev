/**
 * Submission module - student solutions for homework assignments.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.submission.SubmissionApi} - create, list, get, delete submissions</li>
 *   <li>{@link com.example.interhubdev.submission.HomeworkSubmissionDto} - submission DTO</li>
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
 *   <li>document - HomeworkApi (validate homework exists), DocumentApi (validate stored files), StoredFileDto</li>
 *   <li>auth - current user</li>
 *   <li>user - roles for permission checks</li>
 *   <li>error - AppException, Errors</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Submission",
    allowedDependencies = {"document", "auth", "user", "error"}
)
package com.example.interhubdev.submission;
