package com.example.interhubdev.offering;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for looking up timeslot information from the Schedule module.
 * Used by the Offering module to get day-of-week data for lesson generation
 * and to validate timeslot existence when creating offering slots.
 * <p>
 * Implemented by an adapter in the adapter package using ScheduleApi.
 */
public interface TimeslotLookupPort {

    /**
     * Minimal timeslot information needed for lesson date calculation.
     *
     * @param id timeslot ID
     * @param dayOfWeek day of week (1 = Monday ... 7 = Sunday)
     */
    record TimeslotInfo(UUID id, int dayOfWeek) {}

    /**
     * Find timeslot info by ID.
     *
     * @param timeslotId timeslot ID
     * @return timeslot info if found
     */
    Optional<TimeslotInfo> findById(UUID timeslotId);

    /**
     * Check if timeslot exists.
     *
     * @param timeslotId timeslot ID
     * @return true if timeslot exists
     */
    boolean existsById(UUID timeslotId);
}
