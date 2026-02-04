package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.TimeslotDto;

final class ScheduleMappers {

    private ScheduleMappers() {
    }

    static RoomDto toRoomDto(Room e) {
        return new RoomDto(
                e.getId(),
                e.getBuilding(),
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
