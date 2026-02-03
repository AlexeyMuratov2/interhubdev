package com.example.interhubdev.account;

import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserPage;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.UUID;

/**
 * Public API for the Account module. Manages own profile and user management (list, update, delete).
 */
public interface AccountApi {

    /**
     * Get current authenticated user from request.
     */
    Optional<UserDto> getCurrentUser(HttpServletRequest request);

    /**
     * List users with cursor-based pagination. Max 30 per page.
     *
     * @param cursor optional cursor (last user id from previous page); null for first page
     * @param limit  max items (capped at 30)
     * @return page with items and optional next cursor
     */
    UserPage listUsers(UUID cursor, int limit);

    /**
     * Get user by ID.
     */
    Optional<UserDto> getUser(UUID id);

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
     *
     * @param userId  user ID
     * @param request profile fields and roles (all optional)
     * @return updated user
     */
    UserDto updateUser(UUID userId, UpdateUserRequest request);

    /**
     * Delete user. Only ADMIN/SUPER_ADMIN. Cannot delete self. SUPER_ADMIN can only be deleted by another SUPER_ADMIN.
     *
     * @param userId      user to delete
     * @param currentUser current authenticated user (for permission checks)
     */
    void deleteUser(UUID userId, UserDto currentUser);
}
