package com.example.interhubdev.schedule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port for checking group existence and getting group summaries. Used when returning lessons by group:
 * if the group does not exist, return 404 instead of empty array.
 * Implemented by an adapter that delegates to the Group module.
 */
public interface GroupLookupPort {

    /**
     * Check whether a group with the given id exists.
     *
     * @param groupId group id
     * @return true if the group exists
     */
    boolean existsById(UUID groupId);

    /**
     * Get group summaries by ids (batch). Missing ids are absent from the map.
     *
     * @param groupIds group ids (must not be null)
     * @return map groupId -> GroupSummaryDto
     */
    Map<UUID, GroupSummaryDto> getGroupSummaries(List<UUID> groupIds);
}
