package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.RoomExistsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ScheduleRoomService implements RoomExistsPort {

    private final RoomRepository roomRepository;

    @Override
    public boolean existsById(UUID roomId) {
        return roomRepository.existsById(roomId);
    }

    Optional<RoomDto> findById(UUID id) {
        return roomRepository.findById(id).map(ScheduleMappers::toRoomDto);
    }

    List<RoomDto> findAll() {
        return roomRepository.findAllByOrderByBuildingAscNumberAsc().stream()
                .map(ScheduleMappers::toRoomDto)
                .toList();
    }

    @Transactional
    RoomDto create(String building, String number, Integer capacity, String type) {
        if (building == null || building.isBlank()) {
            throw Errors.badRequest("Room building is required");
        }
        if (number == null || number.isBlank()) {
            throw Errors.badRequest("Room number is required");
        }
        if (capacity != null && capacity < 0) {
            throw Errors.badRequest("Room capacity must be >= 0");
        }
        Room entity = Room.builder()
                .building(building.trim())
                .number(number.trim())
                .capacity(capacity)
                .type(type != null ? type.trim() : null)
                .build();
        return ScheduleMappers.toRoomDto(roomRepository.save(entity));
    }

    @Transactional
    RoomDto update(UUID id, String building, String number, Integer capacity, String type) {
        Room entity = roomRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Room not found: " + id));
        if (building != null) entity.setBuilding(building.trim());
        if (number != null) entity.setNumber(number.trim());
        if (capacity != null) {
            if (capacity < 0) throw Errors.badRequest("Room capacity must be >= 0");
            entity.setCapacity(capacity);
        }
        if (type != null) entity.setType(type.trim());
        entity.setUpdatedAt(LocalDateTime.now());
        return ScheduleMappers.toRoomDto(roomRepository.save(entity));
    }

    @Transactional
    void delete(UUID id) {
        if (!roomRepository.existsById(id)) {
            throw Errors.notFound("Room not found: " + id);
        }
        roomRepository.deleteById(id);
    }
}
