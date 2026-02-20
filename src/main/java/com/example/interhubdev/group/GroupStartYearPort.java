package com.example.interhubdev.group;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for getting the start year of a group without pulling in full GroupApi dependencies.
 * Used by adapters to avoid circular dependencies.
 * <p>
 * Implemented by an internal service that only depends on the group repository.
 */
public interface GroupStartYearPort {

    /**
     * Get start year for the given group.
     *
     * @param groupId group ID
     * @return optional start year if group exists
     */
    Optional<Integer> getStartYearByGroupId(UUID groupId);
}
