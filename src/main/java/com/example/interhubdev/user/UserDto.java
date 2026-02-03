package com.example.interhubdev.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for User.
 * Used to expose user data to other modules without exposing the entity.
 * A user may have multiple roles; at most one of STAFF, ADMIN, SUPER_ADMIN.
 */
public record UserDto(
    UUID id,
    String email,
    Set<Role> roles,
    UserStatus status,
    String firstName,
    String lastName,
    String phone,
    LocalDate birthDate,
    LocalDateTime createdAt,
    LocalDateTime activatedAt,
    LocalDateTime lastLoginAt
) {
    /**
     * Check if user can log in.
     */
    public boolean canLogin() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * Check if user has the given role.
     */
    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return email;
        }
        return String.format("%s %s",
            firstName != null ? firstName : "",
            lastName != null ? lastName : "").trim();
    }
}
