package com.example.interhubdev.schedule;

import java.util.List;
import java.util.UUID;

/**
 * Port for offering lookup. Implemented by the Offering adapter so that
 * the Schedule module does not depend on the Offering module (dependency inversion).
 */
public interface OfferingLookupPort {

    boolean existsById(UUID offeringId);

    /**
     * Ids of offerings that belong to the given group (for filtering lessons by group).
     *
     * @param groupId group ID
     * @return list of offering IDs; empty if group has no offerings or group does not exist
     */
    List<UUID> findOfferingIdsByGroupId(UUID groupId);
}
