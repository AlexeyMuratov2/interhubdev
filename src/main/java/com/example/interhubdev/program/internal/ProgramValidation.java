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

    private ProgramValidation() {
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

