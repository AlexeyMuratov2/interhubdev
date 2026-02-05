package com.example.interhubdev.account;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of students with display names.
 */
public record StudentListPage(
        List<StudentProfileItem> items,
        UUID nextCursor
) {
    public boolean hasNext() {
        return nextCursor != null;
    }
}
