package com.example.interhubdev.group.internal;

import com.example.interhubdev.error.Errors;

/**
 * Internal validation helpers for Group module.
 */
final class GroupValidation {

    static final int MIN_YEAR = 1900;
    static final int MAX_YEAR = 2100;

    private GroupValidation() {
    }

    static String requiredTrimmed(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw Errors.badRequest(fieldName + " is required");
        }
        return value.trim();
    }

    static void validateYearRange(int year, String fieldName) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw Errors.badRequest(fieldName + " must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
    }

    static String normalizeLeaderRole(String role) {
        String normalized = requiredTrimmed(role, "Role").toLowerCase();
        if (!"headman".equals(normalized) && !"deputy".equals(normalized)) {
            throw Errors.badRequest("Role must be headman or deputy");
        }
        return normalized;
    }

    static String normalizeOverrideAction(String action) {
        String normalized = requiredTrimmed(action, "Action").toUpperCase();
        if (!"ADD".equals(normalized) && !"REMOVE".equals(normalized) && !"REPLACE".equals(normalized)) {
            throw Errors.badRequest("Action must be ADD, REMOVE, or REPLACE");
        }
        return normalized;
    }
}

