package com.example.interhubdev.program;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for getting the start year of a group.
 * Implemented by the adapter using the Group module so that Program does not depend on Group.
 *
 * @see com.example.interhubdev.adapter.GroupStartYearAdapter
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
