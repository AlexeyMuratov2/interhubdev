package com.example.interhubdev.adapter;

import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.subject.port.SlotLessonsCheckPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter: implements Subject's SlotLessonsCheckPort using Offering and Schedule modules.
 * Checks whether at least one offering has at least one slot with at least one lesson.
 */
@Component
@RequiredArgsConstructor
public class SubjectSlotLessonsCheckAdapter implements SlotLessonsCheckPort {

    private final OfferingApi offeringApi;
    private final ScheduleApi scheduleApi;

    @Override
    public boolean hasAtLeastOneLessonForOfferings(Collection<UUID> offeringIds) {
        if (offeringIds == null || offeringIds.isEmpty()) {
            return false;
        }
        List<UUID> slotIds = new ArrayList<>();
        for (UUID offeringId : offeringIds) {
            offeringApi.findSlotsByOfferingId(offeringId).stream()
                    .map(s -> s.id())
                    .forEach(slotIds::add);
        }
        if (slotIds.isEmpty()) {
            return false;
        }
        Set<UUID> slotIdsWithLessons = scheduleApi.findOfferingSlotIdsWithAtLeastOneLesson(slotIds);
        return !slotIdsWithLessons.isEmpty();
    }
}
