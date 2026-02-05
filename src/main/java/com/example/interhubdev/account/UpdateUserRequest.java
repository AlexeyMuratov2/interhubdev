package com.example.interhubdev.account;

import com.example.interhubdev.student.CreateStudentRequest;
import com.example.interhubdev.teacher.CreateTeacherRequest;
import com.example.interhubdev.user.Role;

import java.time.LocalDate;
import java.util.Set;

/**
 * Request for updating a user by moderator/admin.
 * <p>
 * All top-level fields are optional (PATCH semantics). When adding role STUDENT or TEACHER,
 * the admin can provide {@link #studentProfile} or {@link #teacherProfile} in the same request;
 * if not provided, no profile is created (admin or user must add it later via dedicated endpoints).
 * Nested profile objects support partial data for updates; required fields (studentId, faculty / teacherId, faculty)
 * are enforced only when creating a new profile. Email cannot be changed.
 */
public record UpdateUserRequest(
        String firstName,
        String lastName,
        String phone,
        LocalDate birthDate,
        Set<Role> roles,
        CreateStudentRequest studentProfile,
        CreateTeacherRequest teacherProfile
) {
    /**
     * Creates a request without nested profiles (roles and basic profile only). Backward compatible.
     */
    public static UpdateUserRequest of(String firstName, String lastName, String phone, LocalDate birthDate, Set<Role> roles) {
        return new UpdateUserRequest(firstName, lastName, phone, birthDate, roles, null, null);
    }
}
