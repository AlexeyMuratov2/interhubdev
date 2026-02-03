/**
 * Subject module - catalog of subjects and assessment types.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.subject.SubjectApi} - subjects and assessment types</li>
 *   <li>{@link com.example.interhubdev.subject.SubjectDto} - subject DTO</li>
 *   <li>{@link com.example.interhubdev.subject.AssessmentTypeDto} - assessment type DTO</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Create, update and delete operations for subjects and assessment types are allowed
 * only for roles: STAFF, ADMIN, SUPER_ADMIN. Teachers and students can only read.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 * </ul>
 *
 * <h2>Error codes (via {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>NOT_FOUND (404) - subject or assessment type not found</li>
 *   <li>CONFLICT (409) - subject or assessment type with given code already exists</li>
 *   <li>BAD_REQUEST (400) - code is blank or invalid input</li>
 *   <li>VALIDATION_FAILED (400) - request validation failed (@Valid on create)</li>
 *   <li>FORBIDDEN (403) - user has no STAFF/ADMIN/SUPER_ADMIN role for write operations</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Subject",
    allowedDependencies = {"error"}
)
package com.example.interhubdev.subject;
