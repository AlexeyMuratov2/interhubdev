/**
 * Teacher module - manages teacher profiles for users with TEACHER role.
 * 
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.teacher.TeacherApi} - main interface for teacher operations</li>
 *   <li>{@link com.example.interhubdev.teacher.TeacherDto} - teacher data transfer object</li>
 *   <li>{@link com.example.interhubdev.teacher.CreateTeacherRequest} - request for creating teacher profile</li>
 * </ul>
 * 
 * <h2>Relationship with User</h2>
 * Each Teacher profile is linked to exactly one User with role=TEACHER (OneToOne).
 * The User entity contains authentication data, while Teacher contains academic data.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Teacher",
    allowedDependencies = {"user"}
)
package com.example.interhubdev.teacher;
