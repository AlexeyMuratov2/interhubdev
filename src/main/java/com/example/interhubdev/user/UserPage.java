package com.example.interhubdev.user;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of users.
 */
public record UserPage(
        List<UserDto> items,
        UUID nextCursor
) {
    /**
     * True if there are more items (next page available).
     */
    public boolean hasNext() {
        return nextCursor != null;
    }
}
