package com.example.interhubdev.account;

import java.time.LocalDate;

/**
 * Request for updating own profile or profile fields. All fields optional (PATCH). Email cannot be changed.
 */
public record UpdateProfileRequest(
        String firstName,
        String lastName,
        String phone,
        LocalDate birthDate
) {
}
