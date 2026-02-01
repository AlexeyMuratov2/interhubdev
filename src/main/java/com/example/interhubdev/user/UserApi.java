package com.example.interhubdev.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
     *
     * @param email     user email (must be unique)
     * @param role      user role
     * @param firstName first name (optional)
     * @param lastName  last name (optional)
     * @return created user
     * @throws IllegalArgumentException if email already exists
     */
    UserDto createUser(String email, Role role, String firstName, String lastName);

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
}
