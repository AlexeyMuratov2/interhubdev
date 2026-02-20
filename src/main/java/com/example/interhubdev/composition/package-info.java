/**
 * Composition module - read-only data aggregation for complex UI screens.
 *
 * <h2>Purpose</h2>
 * This module aggregates data from multiple modules into a single response to reduce
 * the number of frontend requests in complex scenarios. It follows a "read-only composition"
 * pattern: it does not contain business logic or modify data, only reads and composes.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.composition.CompositionApi} - data composition facade</li>
 *   <li>{@link com.example.interhubdev.composition.LessonFullDetailsDto} - aggregated lesson details container</li>
 *   <li>{@link com.example.interhubdev.composition.LessonRosterAttendanceDto} - roster attendance for lesson screen table</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * The module reuses DTOs from other modules (schedule, offering, subject, group, document, teacher, program).
 * It does not create new "view DTOs" except for a container/wrapper that aggregates existing DTOs.
 * Each endpoint represents one use case (one endpoint = one use case).
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Use Case #1: Lesson Full Details - aggregates all data needed for the "Full Lesson Information" screen</li>
 *   <li>Use Case #2: Lesson Roster Attendance - students in group + attendance status + absence notices for lesson screen table</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>schedule - lesson, room information</li>
 *   <li>offering - offering information and offering teachers</li>
 *   <li>subject - subject information</li>
 *   <li>group - group information</li>
 *   <li>document - lesson materials and homework</li>
 *   <li>teacher - teacher information</li>
 *   <li>program - curriculum subject information</li>
 *   <li>auth - current user for authentication</li>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 *   <li>student - roster by group for attendance table</li>
 *   <li>attendance - session attendance and absence notices</li>
 *   <li>grades - points per student for this lesson</li>
 * </ul>
 *
 * <h2>Error codes (via {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>NOT_FOUND (404) - lesson, offering, subject, group, room, teacher, curriculum subject not found</li>
 *   <li>UNAUTHORIZED (401) - authentication required</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Composition",
    allowedDependencies = {"schedule", "offering", "subject", "group", "document", "teacher", "program", "auth", "error", "student", "attendance", "grades"}
)
package com.example.interhubdev.composition;
