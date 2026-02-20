package com.example.interhubdev.attendance;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of absence notices for teacher dashboard, with enriched context per item.
 */
public record TeacherAbsenceNoticePage(
        List<TeacherAbsenceNoticeItemDto> items,
        UUID nextCursor
) {
    public boolean hasNext() {
        return nextCursor != null;
    }
}
