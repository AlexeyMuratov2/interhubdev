package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.schedule.BuildingDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.RoomSummaryDto;
import com.example.interhubdev.schedule.TimeslotDto;

/** Entity to DTO mapping for schedule module. Do not instantiate. */
final class ScheduleMappers {

    private ScheduleMappers() {
    }

    static BuildingDto toBuildingDto(Building e) {
        return new BuildingDto(
                e.getId(),
                e.getName(),
                e.getAddress(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    static RoomDto toRoomDto(Room e) {
        Building b = e.getBuilding();
        return new RoomDto(
                e.getId(),
                b.getId(),
                b.getName(),
                e.getNumber(),
                e.getCapacity(),
                e.getType(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    static RoomSummaryDto toRoomSummaryDto(Room e) {
        Building b = e.getBuilding();
        return new RoomSummaryDto(e.getId(), e.getNumber(), b != null ? b.getName() : null);
    }

    static TimeslotDto toTimeslotDto(Timeslot e) {
        return new TimeslotDto(
                e.getId(),
                e.getDayOfWeek(),
                e.getStartTime(),
                e.getEndTime()
        );
    }

    static LessonDto toLessonDto(Lesson e) {
        String status = e.getStatus();
        status = status != null && !status.isBlank() ? status.trim().toUpperCase() : ScheduleValidation.DEFAULT_LESSON_STATUS;
        return new LessonDto(
                e.getId(),
                e.getOfferingId(),
                e.getOfferingSlotId(),
                e.getDate(),
                e.getStartTime(),
                e.getEndTime(),
                e.getTimeslotId(),
                e.getRoomId(),
                e.getTopic(),
                status,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
