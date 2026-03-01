package com.example.interhubdev.absencenotice;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of absence notices for student dashboard.
 */
public record StudentAbsenceNoticePage(
        List<StudentAbsenceNoticeItemDto> items,
        UUID nextCursor
) {
    public boolean hasNext() {
        return nextCursor != null;
    }
}
