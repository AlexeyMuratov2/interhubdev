package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.schedule.BuildingDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.TimeslotDto;

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

    static TimeslotDto toTimeslotDto(Timeslot e) {
        return new TimeslotDto(
                e.getId(),
                e.getDayOfWeek(),
                e.getStartTime(),
                e.getEndTime()
        );
    }

    static LessonDto toLessonDto(Lesson e) {
        return new LessonDto(
                e.getId(),
                e.getOfferingId(),
                e.getDate(),
                e.getTimeslotId(),
                e.getRoomId(),
                e.getTopic(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
