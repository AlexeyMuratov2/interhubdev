package com.example.interhubdev.group;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port for getting group summaries (id, code, name) without pulling in full GroupApi dependencies.
 * Used by adapters to avoid circular dependencies.
 * <p>
 * Implemented by an internal service that only depends on the group repository.
 */
public interface GroupSummaryPort {

    /**
     * Get group summaries by ids (batch). Missing ids are absent from the map.
     *
     * @param groupIds group ids (must not be null)
     * @return map groupId -> GroupSummary (id, code, name); missing ids are absent from map
     */
    Map<UUID, GroupSummary> getGroupSummaries(List<UUID> groupIds);

    /**
     * Group summary data (id, code, name).
     */
    record GroupSummary(UUID id, String code, String name) {}
}
