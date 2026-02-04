package com.example.interhubdev.subject.internal;

import com.example.interhubdev.error.Errors;

/**
 * Validation and normalization helpers for subject module.
 * Ensures codes and names are trimmed and required fields are present; throws via {@link Errors} on violation.
 * Name fields: chineseName (required), englishName (optional).
 */
final class SubjectValidation {

    private SubjectValidation() {
    }

    /**
     * Trims subject/assessment type code and ensures it is not null or blank.
     *
     * @param code raw code (may be null or blank)
     * @return trimmed code
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if code is null or blank
     */
    static String requireTrimmedCode(String code) {
        if (code == null || code.isBlank()) {
            throw Errors.badRequest("Subject code is required");
        }
        return code.trim();
    }

    /**
     * Trims assessment type code and ensures it is not null or blank.
     *
     * @param code raw code (may be null or blank)
     * @return trimmed code
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if code is null or blank
     */
    static String requireTrimmedAssessmentTypeCode(String code) {
        if (code == null || code.isBlank()) {
            throw Errors.badRequest("Assessment type code is required");
        }
        return code.trim();
    }

    /**
     * Returns trimmed Chinese name, or empty string if null (for non-nullable chineseName field).
     */
    static String trimChineseName(String name) {
        return name != null ? name.trim() : "";
    }

    /**
     * Returns trimmed English name, or null if null/blank (for optional englishName field).
     */
    static String trimEnglishName(String name) {
        return (name == null || name.isBlank()) ? null : name.trim();
    }

    /**
     * Returns trimmed description, or null if input is null/blank.
     */
    static String trimDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
