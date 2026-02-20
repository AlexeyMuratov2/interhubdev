/**
 * Program module - programs, curricula, and curriculum subjects (Layer 1-2).
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.program.ProgramApi} - programs, curricula, curriculum subjects</li>
 *   <li>{@link com.example.interhubdev.program.SemesterIdByYearPort} - port for resolving semester ID by calendar year and number (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.program.GroupStartYearPort} - port for getting group start year (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.program.GroupCurriculumIdPort} - port for getting curriculum ID from group (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.program.ProgramDto}, {@link com.example.interhubdev.program.CurriculumDto}, {@link com.example.interhubdev.program.CurriculumSubjectDto} - DTOs</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Create, update and delete operations for programs, curricula and curriculum subjects are allowed
 * Write operations only for roles: MODERATOR, ADMIN, SUPER_ADMIN. STAFF can only read. Teachers and students can only read.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>department - program may reference a department</li>
 *   <li>subject - curriculum subjects reference subjects and assessment types</li>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 * </ul>
 *
 * <h2>Error codes (via {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>NOT_FOUND (404) - program, curriculum, curriculum subject, department, subject or assessment type not found; group not found; semester not found for group course and semester</li>
 *   <li>CONFLICT (409) - program code already exists; curriculum version already exists for program; curriculum subject duplicate</li>
 *   <li>BAD_REQUEST (400) - code/version required; durationYears/durationWeeks out of range; semesterNo must be 1 or 2; courseYear out of curriculum range (exceeds durationYears); required ids missing</li>
 *   <li>VALIDATION_FAILED (400) - request validation failed (@Valid on create/update)</li>
 *   <li>FORBIDDEN (403) - user has no MODERATOR/ADMIN/SUPER_ADMIN role for write operations</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Program",
    allowedDependencies = {"department", "subject", "error"}
)
package com.example.interhubdev.program;
