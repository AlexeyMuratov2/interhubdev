package com.example.interhubdev.offering;

import java.util.List;
import java.util.UUID;

/**
 * Port exposed by Offering module: minimal lookup data for adapters (existence and offering IDs by group or teacher).
 * Implemented by a service that only depends on the offering repositories, so it does not pull in
 * Schedule or other modules. Used by the Schedule adapter to avoid circular dependency.
 */
public interface OfferingLookupDataPort {

    boolean existsById(UUID offeringId);

    /**
     * Ids of offerings that belong to the given group.
     *
     * @param groupId group ID
     * @return list of offering IDs; empty if group has no offerings
     */
    List<UUID> findOfferingIdsByGroupId(UUID groupId);

    /**
     * Ids of offerings where the given teacher is assigned (as main teacher, slot teacher, or offering teacher).
     *
     * @param teacherId teacher entity ID
     * @return list of offering IDs; empty if teacher has no offerings
     */
    List<UUID> findOfferingIdsByTeacherId(UUID teacherId);
}
