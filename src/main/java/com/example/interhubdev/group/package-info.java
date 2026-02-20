/**
 * Group module - student groups, group leaders, curriculum overrides.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.group.GroupApi} - groups, leaders, overrides</li>
 *   <li>{@link com.example.interhubdev.group.GroupExistsPort} - lightweight port for checking group existence (avoids circular dependencies)</li>
 *   <li>{@link com.example.interhubdev.group.GroupSummaryPort} - lightweight port for getting group summaries (id, code, name) (avoids circular dependencies)</li>
 *   <li>{@link com.example.interhubdev.group.GroupStartYearPort} - lightweight port for getting group start year (avoids circular dependencies)</li>
 *   <li>{@link com.example.interhubdev.group.GroupCurriculumIdPort} - lightweight port for getting group curriculum ID (avoids circular dependencies)</li>
 *   <li>{@link com.example.interhubdev.group.StudentGroupDto}, {@link com.example.interhubdev.group.GroupLeaderDto}, {@link com.example.interhubdev.group.GroupCurriculumOverrideDto} - DTOs</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Create, update and delete operations for groups, group leaders and curriculum overrides are allowed
 * Write operations only for roles: MODERATOR, ADMIN, SUPER_ADMIN. STAFF can only read. Teachers and students can only read.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>program - groups reference program and curriculum</li>
 *   <li>student - group leaders and members reference students</li>
 *   <li>teacher - group curator</li>
 *   <li>user - curator and display names for leaders/members</li>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors} / {@link com.example.interhubdev.group.internal.GroupErrors}</li>
 * </ul>
 *
 * <h2>Error codes</h2>
 * Via {@link com.example.interhubdev.group.internal.GroupErrors}: GROUP_NOT_FOUND (404), GROUP_LEADER_NOT_FOUND (404),
 * GROUP_OVERRIDE_NOT_FOUND (404), GROUP_CODE_EXISTS (409), GROUP_LEADER_ROLE_EXISTS (409).
 * Via {@link com.example.interhubdev.error.Errors}: NOT_FOUND for program/curriculum/user/student; BAD_REQUEST, FORBIDDEN, VALIDATION_FAILED.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Group",
    allowedDependencies = {"program", "student", "teacher", "user", "error"}
)
package com.example.interhubdev.group;
