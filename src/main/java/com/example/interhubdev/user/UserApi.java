package com.example.interhubdev.user;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Public API for the User module.
 * Other modules should use this interface to interact with user data.
 */
public interface UserApi {

    /**
     * Find user by ID.
     */
    Optional<UserDto> findById(UUID id);

    /**
     * Find users by IDs.
     * Returns users in arbitrary order; missing IDs are skipped.
     */
    List<UserDto> findByIds(Collection<UUID> ids);

    /**
     * Find user by email.
     */
    Optional<UserDto> findByEmail(String email);

    /**
     * Check if user with given email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Get all users.
     */
    List<UserDto> findAll();

    /**
     * Get users by role.
     */
    List<UserDto> findByRole(Role role);

    /**
     * Get users by status.
     */
    List<UserDto> findByStatus(UserStatus status);

    /**
     * Create a new user with PENDING status.
     * Password will be set during activation.
     * At most one of STAFF, MODERATOR, ADMIN, SUPER_ADMIN is allowed in roles.
     *
     * @param email     user email (must be unique)
     * @param roles     user roles (at least one; at most one of STAFF, MODERATOR, ADMIN, SUPER_ADMIN)
     * @param firstName first name (optional)
     * @param lastName  last name (optional)
     * @return created user
     * @throws IllegalArgumentException if email already exists or roles invalid
     */
    UserDto createUser(String email, Collection<Role> roles, String firstName, String lastName);

    /**
     * Activate user account by setting password.
     * Changes status from PENDING to ACTIVE.
     * Password encoding is handled internally.
     *
     * @param userId      user ID
     * @param rawPassword plain text password (will be BCrypt encoded)
     * @throws IllegalArgumentException if user not found
     * @throws IllegalStateException    if user is not in PENDING status
     */
    void activateUser(UUID userId, String rawPassword);

    /**
     * Set a new password for an active user (e.g. password recovery).
     * Only allowed when user is in ACTIVE status. Password encoding is handled internally.
     *
     * @param userId      user ID
     * @param rawPassword plain text password (will be BCrypt encoded)
     * @throws IllegalArgumentException if user not found
     * @throws IllegalStateException    if user is not in ACTIVE status
     */
    void setPassword(UUID userId, String rawPassword);

    /**
     * Disable user account.
     *
     * @param userId user ID
     * @throws IllegalArgumentException if user not found
     */
    void disableUser(UUID userId);

    /**
     * Enable previously disabled user account.
     *
     * @param userId user ID
     * @throws IllegalArgumentException if user not found
     * @throws IllegalStateException    if user has no password set
     */
    void enableUser(UUID userId);

    /**
     * Set user back to PENDING for re-invitation (e.g. after cancelled invitation).
     * Only allowed when user is DISABLED and has no password set.
     *
     * @param userId user ID
     * @throws IllegalArgumentException if user not found
     * @throws IllegalStateException    if user is not DISABLED or already has password
     */
    void reactivateForReinvite(UUID userId);

    /**
     * Verify user password for authentication.
     * This method encapsulates password verification within the user module,
     * preventing password hash from leaking to other modules.
     *
     * @param email       user email
     * @param rawPassword plain text password to verify
     * @return true if password matches and user can login, false otherwise
     */
    boolean verifyPassword(String email, String rawPassword);

    /**
     * Update user's last login timestamp.
     * Called by auth module after successful authentication.
     *
     * @param userId user ID
     * @throws IllegalArgumentException if user not found
     */
    void updateLastLoginAt(UUID userId);

    /**
     * Update profile fields (email is never changed).
     *
     * @param userId    user ID
     * @param firstName first name (optional)
     * @param lastName  last name (optional)
     * @param phone     phone (optional)
     * @param birthDate birth date (optional)
     * @return updated user
     * @throws IllegalArgumentException if user not found
     */
    UserDto updateProfile(UUID userId, String firstName, String lastName, String phone, LocalDate birthDate);

    /**
     * Update user roles. At most one of STAFF, MODERATOR, ADMIN, SUPER_ADMIN is allowed.
     *
     * @param userId user ID
     * @param roles  new roles (at least one)
     * @return updated user
     * @throws IllegalArgumentException if user not found or roles invalid
     */
    UserDto updateRoles(UUID userId, Set<Role> roles);

    /**
     * Delete user from the database. Caller must ensure related data (tokens, profiles, invitations) is cleaned up first.
     *
     * @param userId user ID
     * @throws IllegalArgumentException if user not found
     */
    void deleteUser(UUID userId);

    /**
     * List users with cursor-based pagination. Ordered by id ascending.
     *
     * @param cursor optional cursor (last user id from previous page); null for first page
     * @param limit  max items per page (will be capped at 30)
     * @return page with items and optional next cursor
     */
    UserPage listUsers(UUID cursor, int limit);
}
