package com.example.interhubdev.program.internal;

import com.example.interhubdev.error.Errors;

import java.math.BigDecimal;

/**
 * Internal validation helpers for Program module.
 * <p>
 * Keep business-invariant checks in services; use these helpers to avoid duplication and
 * to keep error semantics consistent (via {@link Errors}).
 */
final class ProgramValidation {

    static final int MIN_YEAR = 1900;
    static final int MAX_YEAR = 2100;

    /** Allowed semester number: 1 (first) or 2 (second semester within a year). */
    static final int SEMESTER_NO_MIN = 1;
    static final int SEMESTER_NO_MAX = 2;

    private ProgramValidation() {
    }

    /**
     * Validates that semester number is 1 or 2 (exactly two semesters per academic year).
     *
     * @param value     semester number
     * @param fieldName field name for error message
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if value is not 1 or 2
     */
    static void validateSemesterNoOneOrTwo(int value, String fieldName) {
        if (value < SEMESTER_NO_MIN || value > SEMESTER_NO_MAX) {
            throw Errors.badRequest(fieldName + " must be 1 or 2");
        }
    }

    /**
     * Validates course year against curriculum start/end years: 1 <= courseYear <= (endYear - startYear)
     * when endYear is set; otherwise only courseYear >= 1.
     *
     * @param startYear  curriculum start year
     * @param endYear    curriculum end year (nullable)
     * @param courseYear course year to validate (must be non-null when calling)
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if out of range
     */
    static void validateCourseYearAgainstCurriculum(int startYear, Integer endYear, int courseYear) {
        if (courseYear < 1) {
            throw Errors.badRequest("courseYear must be at least 1");
        }
        if (endYear != null && courseYear > (endYear - startYear)) {
            throw Errors.badRequest("courseYear must not exceed curriculum duration (endYear - startYear), max is " + (endYear - startYear));
        }
    }

    static String requiredTrimmed(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw Errors.badRequest(fieldName + " is required");
        }
        return value.trim();
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw Errors.badRequest(message);
        }
    }

    static void validateYearRange(int year, String fieldName) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw Errors.badRequest(fieldName + " must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
    }

    static void validateOptionalYearRange(Integer year, String fieldName) {
        if (year == null) return;
        validateYearRange(year, fieldName);
    }

    static void validateNonNegative(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw Errors.badRequest(fieldName + " must be >= 0");
        }
    }

    static void validatePositive(int value, String fieldName) {
        if (value < 1) {
            throw Errors.badRequest(fieldName + " must be >= 1");
        }
    }

    static void validateWeight01(BigDecimal weight) {
        if (weight == null) return;
        if (weight.compareTo(BigDecimal.ZERO) < 0 || weight.compareTo(BigDecimal.ONE) > 0) {
            throw Errors.badRequest("Weight must be between 0 and 1");
        }
    }
}

