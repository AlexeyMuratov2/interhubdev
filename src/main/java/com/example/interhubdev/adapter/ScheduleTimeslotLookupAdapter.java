package com.example.interhubdev.adapter;

import com.example.interhubdev.offering.TimeslotLookupPort;
import com.example.interhubdev.schedule.ScheduleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter: implements Offering module's TimeslotLookupPort using Schedule module's ScheduleApi.
 * Provides timeslot existence check and day-of-week info for lesson generation.
 */
@Component
@RequiredArgsConstructor
public class ScheduleTimeslotLookupAdapter implements TimeslotLookupPort {

    private final ScheduleApi scheduleApi;

    @Override
    public Optional<TimeslotInfo> findById(UUID timeslotId) {
        return scheduleApi.findTimeslotById(timeslotId)
                .map(ts -> new TimeslotInfo(ts.id(), ts.dayOfWeek()));
    }

    @Override
    public boolean existsById(UUID timeslotId) {
        return scheduleApi.findTimeslotById(timeslotId).isPresent();
    }
}
