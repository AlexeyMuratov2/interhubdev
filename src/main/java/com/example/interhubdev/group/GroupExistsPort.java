package com.example.interhubdev.group;

import java.util.UUID;

/**
 * Port for checking group existence without pulling in full GroupApi dependencies.
 * Used by adapters to avoid circular dependencies.
 * <p>
 * Implemented by an internal service that only depends on the group repository.
 */
public interface GroupExistsPort {

    /**
     * Check if a group exists by ID.
     *
     * @param groupId group ID
     * @return true if group exists, false otherwise
     */
    boolean existsById(UUID groupId);
}
