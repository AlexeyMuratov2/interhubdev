package com.example.interhubdev.account;

import com.example.interhubdev.student.CreateStudentRequest;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.teacher.CreateTeacherRequest;
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserPage;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.UUID;

/**
 * Public API for the Account module. Manages own profile and user management (list, update, delete).
 * Also exposes teachers and students lists and get-one; only the account owner can edit their teacher/student profiles.
 */
public interface AccountApi {

    /**
     * Get current authenticated user from request.
     */
    Optional<UserDto> getCurrentUser(HttpServletRequest request);

    /**
     * List users with cursor-based pagination. Max 30 per page.
     * Returns plain {@link UserDto} only; no optional profile fields.
     *
     * @param cursor optional cursor (last user id from previous page); null for first page
     * @param limit  max items (capped at 30)
     * @return page with items and optional next cursor
     */
    UserPage listUsers(UUID cursor, int limit);

    /**
     * Get user by ID (plain user, no profiles).
     */
    Optional<UserDto> getUser(UUID id);

    /**
     * Get user by ID with all role-specific profiles (teacher, student).
     * Used by GET /api/account/users/{id}. Admin/mod can read any user.
     */
    Optional<UserWithProfilesDto> getUserWithProfiles(UUID id);

    /**
     * Update profile (firstName, lastName, phone, birthDate). Email is never changed.
     *
     * @param userId  user ID (must be current user for self-update, or caller must have MODERATOR/ADMIN/SUPER_ADMIN)
     * @param request profile fields (all optional)
     * @return updated user
     */
    UserDto updateProfile(UUID userId, UpdateProfileRequest request);

    /**
     * Update user (profile + roles) by moderator/admin. Email is never changed.
     * Only SUPER_ADMIN can change teacherId/studentId in role profiles.
     *
     * @param userId      user ID to update
     * @param request     profile fields and roles (all optional)
     * @param editorUserId ID of the user performing the update (for profile-id change permission)
     * @return updated user
     */
    UserDto updateUser(UUID userId, UpdateUserRequest request, UUID editorUserId);

    /**
     * Delete user. Only ADMIN/SUPER_ADMIN. Cannot delete self. SUPER_ADMIN can only be deleted by another SUPER_ADMIN.
     *
     * @param userId      user to delete
     * @param currentUser current authenticated user (for permission checks)
     */
    void deleteUser(UUID userId, UserDto currentUser);

    // --------------- Teachers (read: mod/admin; edit: only owner) ---------------

    /**
     * List teachers with cursor-based pagination. Items include display name.
     */
    TeacherListPage listTeachers(UUID cursor, int limit);

    /**
     * Get teacher profile by user ID. Includes display name.
     */
    Optional<TeacherProfileItem> getTeacher(UUID userId);

    /**
     * Update current user's teacher profile. Only the account owner may call this; admin cannot edit teacher data.
     *
     * @param currentUserId must be the authenticated user's id
     * @param request       partial update (null fields ignored)
     * @return updated teacher profile
     */
    TeacherDto updateMyTeacherProfile(UUID currentUserId, CreateTeacherRequest request);

    // --------------- Students (read: mod/admin; edit: only owner) ---------------

    /**
     * List students with cursor-based pagination. Items include display name.
     */
    StudentListPage listStudents(UUID cursor, int limit);

    /**
     * Get student profile by user ID. Includes display name.
     */
    Optional<StudentProfileItem> getStudent(UUID userId);

    /**
     * Update current user's student profile. Only the account owner may call this; admin cannot edit student data.
     *
     * @param currentUserId must be the authenticated user's id
     * @param request       partial update (null fields ignored)
     * @return updated student profile
     */
    StudentDto updateMyStudentProfile(UUID currentUserId, CreateStudentRequest request);
}
