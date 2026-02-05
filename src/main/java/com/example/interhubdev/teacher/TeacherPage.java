package com.example.interhubdev.teacher;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of teachers.
 */
public record TeacherPage(
        List<TeacherDto> items,
        UUID nextCursor
) {
    /**
     * True if there are more items (next page available).
     */
    public boolean hasNext() {
        return nextCursor != null;
    }
}
