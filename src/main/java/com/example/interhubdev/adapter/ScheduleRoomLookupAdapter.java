package com.example.interhubdev.adapter;

import com.example.interhubdev.offering.RoomLookupPort;
import com.example.interhubdev.schedule.RoomExistsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter: implements Offering module's RoomLookupPort using Schedule module's RoomExistsPort.
 * Uses minimal port (RoomExistsPort) instead of ScheduleApi to avoid circular dependency.
 */
@Component
@RequiredArgsConstructor
public class ScheduleRoomLookupAdapter implements RoomLookupPort {

    private final RoomExistsPort roomExistsPort;

    @Override
    public boolean existsById(UUID roomId) {
        return roomExistsPort.existsById(roomId);
    }
}
