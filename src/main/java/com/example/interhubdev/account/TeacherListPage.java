package com.example.interhubdev.account;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of teachers with display names.
 */
public record TeacherListPage(
        List<TeacherProfileItem> items,
        UUID nextCursor
) {
    public boolean hasNext() {
        return nextCursor != null;
    }
}
