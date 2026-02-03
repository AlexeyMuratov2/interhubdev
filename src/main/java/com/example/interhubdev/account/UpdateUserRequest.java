package com.example.interhubdev.account;

import com.example.interhubdev.user.Role;

import java.time.LocalDate;
import java.util.Set;

/**
 * Request for updating a user by moderator/admin. Profile fields and roles optional. Email cannot be changed.
 */
public record UpdateUserRequest(
        String firstName,
        String lastName,
        String phone,
        LocalDate birthDate,
        Set<Role> roles
) {
}
