/**
 * Student module - manages student profiles for users with STUDENT role.
 * 
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.student.StudentApi} - main interface for student operations</li>
 *   <li>{@link com.example.interhubdev.student.StudentDto} - student data transfer object</li>
 *   <li>{@link com.example.interhubdev.student.CreateStudentRequest} - request for creating student profile</li>
 * </ul>
 * 
 * <h2>Relationship with User</h2>
 * Each Student profile is linked to exactly one User with role=STUDENT (OneToOne).
 * The User entity contains authentication data, while Student contains academic data.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Student",
    allowedDependencies = {"user"}
)
package com.example.interhubdev.student;
