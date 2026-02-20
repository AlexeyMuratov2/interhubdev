package com.example.interhubdev.program;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for getting the curriculum ID of a group.
 * Implemented by the adapter using the Group module so that Program does not depend on Group.
 *
 * @see com.example.interhubdev.adapter.GroupCurriculumIdAdapter
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
