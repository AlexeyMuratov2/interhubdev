/**
 * Academic module - academic years and semesters (academic calendar).
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.academic.AcademicApi} - academic years and semesters</li>
 *   <li>{@link com.example.interhubdev.academic.AcademicYearDto}, {@link com.example.interhubdev.academic.SemesterDto} - DTOs</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Create and update operations for academic years and semesters are allowed for roles:
 * MODERATOR, ADMIN, SUPER_ADMIN. Delete is restricted to ADMIN and SUPER_ADMIN only.
 * Teachers and students can only read.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 * </ul>
 *
 * <h2>Error codes (via {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>NOT_FOUND (404) - academic year or semester not found</li>
 *   <li>CONFLICT (409) - academic year with name already exists; semester number already exists for this year</li>
 *   <li>BAD_REQUEST (400) - name/dates required; end date not after start date; semester dates outside academic year; invalid weekCount</li>
 *   <li>VALIDATION_FAILED (400) - request validation failed (@Valid on create/update)</li>
 *   <li>FORBIDDEN (403) - user has no required role for the operation</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Academic",
    allowedDependencies = {"error"}
)
package com.example.interhubdev.academic;
