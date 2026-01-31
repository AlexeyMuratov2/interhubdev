package com.example.interhubdev.user;

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
    Optional<User> findById(UUID id);

    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user with given email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Get all users.
     */
    List<User> findAll();

    /**
     * Get users by role.
     */
    List<User> findByRole(Role role);

    /**
     * Get users by status.
     */
    List<User> findByStatus(UserStatus status);

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
    User createUser(String email, Role role, String firstName, String lastName);

    /**
     * Activate user account by setting password.
     * Changes status from PENDING to ACTIVE.
     *
     * @param userId          user ID
     * @param encodedPassword BCrypt encoded password
     * @throws IllegalArgumentException if user not found
     * @throws IllegalStateException    if user is not in PENDING status
     */
    void activateUser(UUID userId, String encodedPassword);

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
     * Save user entity.
     */
    User save(User user);
}
