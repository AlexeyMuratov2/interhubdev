package com.example.interhubdev.attendance;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of absence notices.
 * Sorted by submittedAt descending — newest first.
 */
public record AbsenceNoticePage(
        List<AbsenceNoticeDto> items,
        UUID nextCursor
) {
    /**
     * True if there are more items (next page available).
     */
    public boolean hasNext() {
        return nextCursor != null;
    }
}
