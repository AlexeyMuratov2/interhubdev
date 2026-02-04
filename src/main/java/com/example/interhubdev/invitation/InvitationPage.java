package com.example.interhubdev.invitation;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of invitations.
 * Sorted by send time (createdAt) descending — newest first.
 */
public record InvitationPage(
        List<InvitationDto> items,
        UUID nextCursor
) {
    /**
     * True if there are more items (next page available).
     */
    public boolean hasNext() {
        return nextCursor != null;
    }
}
