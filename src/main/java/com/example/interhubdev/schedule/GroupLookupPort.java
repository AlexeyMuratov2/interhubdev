package com.example.interhubdev.schedule;

import java.util.UUID;

/**
 * Port for checking group existence. Used when returning lessons by group:
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
}
