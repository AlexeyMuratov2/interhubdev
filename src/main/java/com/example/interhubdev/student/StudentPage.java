package com.example.interhubdev.student;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of students.
 */
public record StudentPage(
        List<StudentDto> items,
        UUID nextCursor
) {
    /**
     * True if there are more items (next page available).
     */
    public boolean hasNext() {
        return nextCursor != null;
    }
}
