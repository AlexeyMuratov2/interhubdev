package com.example.interhubdev.attendance;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of absence notices for student dashboard, with enriched context per item (lesson, offering, slot).
 */
public record StudentAbsenceNoticePage(
        List<StudentAbsenceNoticeItemDto> items,
        UUID nextCursor
) {
    /**
     * True if there are more items (next page available).
     */
    public boolean hasNext() {
        return nextCursor != null;
    }
}
