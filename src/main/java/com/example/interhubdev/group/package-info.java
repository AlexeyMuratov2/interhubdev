/**
 * Group module - student groups, group leaders, curriculum overrides.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.group.GroupApi} - groups, leaders, overrides</li>
 *   <li>{@link com.example.interhubdev.group.StudentGroupDto}, {@link com.example.interhubdev.group.GroupLeaderDto}, {@link com.example.interhubdev.group.GroupCurriculumOverrideDto} - DTOs</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Group",
    allowedDependencies = {"program", "student", "teacher"}
)
package com.example.interhubdev.group;
