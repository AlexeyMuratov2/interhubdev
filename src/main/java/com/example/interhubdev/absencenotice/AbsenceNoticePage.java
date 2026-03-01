package com.example.interhubdev.absencenotice;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of absence notices.
 */
public record AbsenceNoticePage(
        List<AbsenceNoticeDto> items,
        UUID nextCursor
) {
    public boolean hasNext() {
        return nextCursor != null;
    }
}
