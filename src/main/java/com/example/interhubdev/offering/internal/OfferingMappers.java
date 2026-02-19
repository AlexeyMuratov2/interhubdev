package com.example.interhubdev.offering.internal;

import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingSlotDto;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity to DTO mapping for offering module entities.
 */
final class OfferingMappers {

    private OfferingMappers() {
    }

    static GroupSubjectOfferingDto toOfferingDto(GroupSubjectOffering e) {
        return new GroupSubjectOfferingDto(
                e.getId(),
                e.getGroupId(),
                e.getCurriculumSubjectId(),
                e.getTeacherId(),
                e.getRoomId(),
                e.getFormat(),
                e.getNotes(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    static OfferingSlotDto toSlotDto(OfferingSlot e) {
        return new OfferingSlotDto(
                e.getId(),
                e.getOfferingId(),
                e.getDayOfWeek(),
                e.getStartTime(),
                e.getEndTime(),
                e.getTimeslotId(),
                e.getLessonType(),
                e.getRoomId(),
                e.getTeacherId(),
                e.getCreatedAt()
        );
    }
}
