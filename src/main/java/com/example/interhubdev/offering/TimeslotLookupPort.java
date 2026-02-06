package com.example.interhubdev.offering;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for looking up timeslot information from the Schedule module.
 * Used to validate timeslot existence and to copy time (day, start, end) when creating offering slot from a slot.
 * <p>
 * Implemented by an adapter in the adapter package using ScheduleApi.
 */
public interface TimeslotLookupPort {

    /**
     * Timeslot info for copying day and time when creating an offering slot from a timeslot.
     */
    record TimeslotInfo(UUID id, int dayOfWeek, LocalTime startTime, LocalTime endTime) {}

    Optional<TimeslotInfo> findById(UUID timeslotId);

    boolean existsById(UUID timeslotId);
}
