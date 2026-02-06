package com.example.interhubdev.offering.internal;

import com.example.interhubdev.error.Errors;

import java.util.List;

final class OfferingValidation {

    static final List<String> VALID_FORMATS = List.of("offline", "online", "mixed");
    static final List<String> VALID_TEACHER_ROLES = List.of("LECTURE", "PRACTICE", "LAB");
    static final List<String> VALID_LESSON_TYPES = List.of("LECTURE", "PRACTICE", "LAB", "SEMINAR");

    private OfferingValidation() {
    }

    /** Returns normalized format (trimmed, lowercased) or null. Throws BAD_REQUEST if non-null and invalid. */
    static String normalizeFormat(String format) {
        if (format == null) {
            return null;
        }
        String normalized = format.trim().toLowerCase();
        if (!VALID_FORMATS.contains(normalized)) {
            throw Errors.badRequest("Format must be offline, online, or mixed");
        }
        return normalized;
    }

    /** Returns normalized role (trimmed, uppercased). Throws BAD_REQUEST if null/blank or invalid. */
    static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw Errors.badRequest("Role is required");
        }
        String normalized = role.trim().toUpperCase();
        if (!VALID_TEACHER_ROLES.contains(normalized)) {
            throw Errors.badRequest("Role must be LECTURE, PRACTICE, or LAB");
        }
        return normalized;
    }

    /** Returns normalized lesson type (trimmed, uppercased). Throws BAD_REQUEST if null/blank or invalid. */
    static String normalizeLessonType(String lessonType) {
        if (lessonType == null || lessonType.isBlank()) {
            throw Errors.badRequest("Lesson type is required");
        }
        String normalized = lessonType.trim().toUpperCase();
        if (!VALID_LESSON_TYPES.contains(normalized)) {
            throw Errors.badRequest("Lesson type must be LECTURE, PRACTICE, LAB, or SEMINAR");
        }
        return normalized;
    }

    static String trimNotes(String notes) {
        return notes != null ? notes.trim() : null;
    }
}
