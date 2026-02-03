/**
 * Group module - student groups, group leaders, curriculum overrides.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.group.GroupApi} - groups, leaders, overrides</li>
 *   <li>{@link com.example.interhubdev.group.StudentGroupDto}, {@link com.example.interhubdev.group.GroupLeaderDto}, {@link com.example.interhubdev.group.GroupCurriculumOverrideDto} - DTOs</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Create, update and delete operations for groups, group leaders and curriculum overrides are allowed
 * only for roles: STAFF, ADMIN, SUPER_ADMIN. Teachers and students can only read.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>program - groups reference program and curriculum</li>
 *   <li>student - group leaders reference students</li>
 *   <li>teacher - group curator</li>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 * </ul>
 *
 * <h2>Error codes (via {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>NOT_FOUND (404) - group, program, curriculum, teacher, student, group leader or override not found</li>
 *   <li>CONFLICT (409) - group code already exists; leader role already exists for group/student</li>
 *   <li>BAD_REQUEST (400) - code/ids/action/role required; startYear out of range; action must be ADD/REMOVE/REPLACE; role must be headman/deputy</li>
 *   <li>VALIDATION_FAILED (400) - request validation failed (@Valid on create)</li>
 *   <li>FORBIDDEN (403) - user has no STAFF/ADMIN/SUPER_ADMIN role for write operations</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Group",
    allowedDependencies = {"program", "student", "teacher", "error"}
)
package com.example.interhubdev.group;
