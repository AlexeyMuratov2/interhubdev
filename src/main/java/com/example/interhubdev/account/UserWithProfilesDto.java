package com.example.interhubdev.account;

import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.UserDto;

/**
 * User with optional role-specific profiles.
 * Returned by GET /api/account/users/{id} to expose all data for all roles (teacher, student).
 * List endpoint (GET /users) is unchanged and returns only {@link UserDto}.
 */
public record UserWithProfilesDto(
        UserDto user,
        TeacherDto teacherProfile,
        StudentDto studentProfile
) {
    /**
     * Create with only user (no teacher/student profiles).
     */
    public static UserWithProfilesDto userOnly(UserDto user) {
        return new UserWithProfilesDto(user, null, null);
    }

    /**
     * Create with user and optional teacher profile.
     */
    public static UserWithProfilesDto withTeacher(UserDto user, TeacherDto teacherProfile) {
        return new UserWithProfilesDto(user, teacherProfile, null);
    }

    /**
     * Create with user and optional student profile.
     */
    public static UserWithProfilesDto withStudent(UserDto user, StudentDto studentProfile) {
        return new UserWithProfilesDto(user, null, studentProfile);
    }

    /**
     * Create with user and both profiles.
     */
    public static UserWithProfilesDto withProfiles(UserDto user, TeacherDto teacherProfile, StudentDto studentProfile) {
        return new UserWithProfilesDto(user, teacherProfile, studentProfile);
    }
}
