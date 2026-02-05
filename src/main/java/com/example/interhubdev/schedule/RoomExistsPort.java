package com.example.interhubdev.schedule;

import java.util.UUID;

/**
 * Port exposed by Schedule module: check room existence without pulling full ScheduleApi.
 * Implemented by ScheduleRoomService; used by adapters to avoid circular dependency.
 */
public interface RoomExistsPort {

    boolean existsById(UUID roomId);
}
