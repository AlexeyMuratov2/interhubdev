package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.Errors;

import java.util.List;

final class ScheduleValidation {

    static final List<String> VALID_LESSON_STATUSES = List.of("planned", "cancelled", "done");
    static final String DEFAULT_LESSON_STATUS = "planned";

    private ScheduleValidation() {
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
