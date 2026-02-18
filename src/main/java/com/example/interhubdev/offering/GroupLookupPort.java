package com.example.interhubdev.offering;

import java.util.UUID;

/**
 * Port for looking up group information from the Group module.
 * Used to validate group existence when creating/updating offerings.
 * <p>
 * Implemented by an adapter in the adapter package using GroupApi.
 */
public interface GroupLookupPort {

    /**
     * Check if a group exists by ID.
     *
     * @param groupId group ID
     * @return true if group exists, false otherwise
     */
    boolean existsById(UUID groupId);
}
