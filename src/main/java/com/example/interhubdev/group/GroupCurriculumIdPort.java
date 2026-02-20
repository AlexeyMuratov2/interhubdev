package com.example.interhubdev.group;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for getting the curriculum ID of a group without pulling in full GroupApi dependencies.
 * Used by adapters to avoid circular dependencies.
 * <p>
 * Implemented by an internal service that only depends on the group repository.
 */
public interface GroupCurriculumIdPort {

    /**
     * Get curriculum ID for the given group.
     *
     * @param groupId group ID
     * @return optional curriculum ID if group exists
     */
    Optional<UUID> getCurriculumIdByGroupId(UUID groupId);
}
