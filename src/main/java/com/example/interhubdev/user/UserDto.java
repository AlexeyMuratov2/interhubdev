package com.example.interhubdev.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for User.
 * Used to expose user data to other modules without exposing the entity.
 */
public record UserDto(
    UUID id,
    String email,
    Role role,
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

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return email;
        }
        return String.format("%s %s", 
            firstName != null ? firstName : "", 
            lastName != null ? lastName : "").trim();
    }
}
