package com.example.interhubdev.absencenotice;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of absence notices for teacher dashboard.
 */
public record TeacherAbsenceNoticePage(
        List<TeacherAbsenceNoticeItemDto> items,
        UUID nextCursor
) {
    public boolean hasNext() {
        return nextCursor != null;
    }
}
