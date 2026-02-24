package com.example.interhubdev.adapter;

import com.example.interhubdev.group.port.GroupIdsByTeacherPort;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.schedule.ScheduleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter: implements Group's GroupIdsByTeacherPort using Offering and Schedule modules.
 * Returns group IDs where the teacher has at least one offering slot with at least one lesson.
 */
@Component
@RequiredArgsConstructor
public class GroupIdsByTeacherAdapter implements GroupIdsByTeacherPort {

    private final OfferingApi offeringApi;
    private final ScheduleApi scheduleApi;

    @Override
    public List<UUID> findGroupIdsByTeacherId(UUID teacherId) {
        List<OfferingSlotDto> slots = offeringApi.findSlotsByTeacherId(teacherId);
        if (slots.isEmpty()) {
            return List.of();
        }
        List<UUID> slotIds = slots.stream().map(OfferingSlotDto::id).toList();
        Set<UUID> slotIdsWithLessons = scheduleApi.findOfferingSlotIdsWithAtLeastOneLesson(slotIds);
        Set<UUID> offeringIdsSet = new LinkedHashSet<>();
        for (OfferingSlotDto slot : slots) {
            if (slotIdsWithLessons.contains(slot.id())) {
                offeringIdsSet.add(slot.offeringId());
            }
        }
        if (offeringIdsSet.isEmpty()) {
            return List.of();
        }
        List<GroupSubjectOfferingDto> offerings = offeringApi.findOfferingsByIds(offeringIdsSet);
        return offerings.stream().map(GroupSubjectOfferingDto::groupId).distinct().toList();
    }
}
