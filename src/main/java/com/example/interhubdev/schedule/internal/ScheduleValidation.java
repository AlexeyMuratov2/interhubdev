package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.Errors;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

final class ScheduleValidation {

    static final List<String> VALID_LESSON_STATUSES = List.of("planned", "cancelled", "done");
    static final String DEFAULT_LESSON_STATUS = "planned";

    private ScheduleValidation() {
    }

    /**
     * Parses date string (yyyy-MM-dd) to LocalDate. Throws BAD_REQUEST if blank or invalid.
     */
    static LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw Errors.badRequest(fieldName + " is required");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw Errors.badRequest("Invalid date format, use ISO-8601 (yyyy-MM-dd)");
        }
    }

    /**
     * Parses time string (HH:mm or HH:mm:ss) to LocalTime. Throws BAD_REQUEST if blank or invalid.
     */
    static LocalTime parseTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw Errors.badRequest(fieldName + " is required");
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw Errors.badRequest("Invalid " + fieldName + " format, use HH:mm or HH:mm:ss");
        }
    }

    /**
     * Parses time string if non-blank; returns null if null or blank. Throws BAD_REQUEST if invalid format.
     */
    static LocalTime parseTimeOptional(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw Errors.badRequest("Invalid " + fieldName + " format, use HH:mm or HH:mm:ss");
        }
    }

    /**
     * Returns normalized lesson status (trimmed, lowercased) or default "planned" if null/blank.
     * Throws BAD_REQUEST if non-null and invalid.
     */
    static String normalizeLessonStatus(String status) {
        if (status == null || status.isBlank()) {
            return DEFAULT_LESSON_STATUS;
        }
        String normalized = status.trim().toLowerCase();
        if (!VALID_LESSON_STATUSES.contains(normalized)) {
            throw Errors.badRequest("Status must be planned, cancelled, or done");
        }
        return normalized;
    }
}
