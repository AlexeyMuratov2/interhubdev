package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.schedule.internal.ScheduleErrors;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.RoomExistsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.interhubdev.schedule.RoomSummaryDto;

/** CRUD for rooms; implements RoomExistsPort for other modules. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ScheduleRoomService implements RoomExistsPort {

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;

    @Override
    public boolean existsById(UUID roomId) {
        return roomRepository.existsById(roomId);
    }

    Optional<RoomDto> findById(UUID id) {
        return roomRepository.findById(id).map(ScheduleMappers::toRoomDto);
    }

    /**
     * Batch load rooms by ids with building (for schedule display). Missing ids are skipped.
     */
    List<RoomSummaryDto> findByIdIn(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return roomRepository.findAllByIdInWithBuilding(ids).stream()
                .map(ScheduleMappers::toRoomSummaryDto)
                .collect(Collectors.toList());
    }

    List<RoomDto> findAll() {
        return roomRepository.findAllByOrderByBuilding_NameAscNumberAsc().stream()
                .map(ScheduleMappers::toRoomDto)
                .toList();
    }

    @Transactional
    RoomDto create(UUID buildingId, String number, Integer capacity, String type) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> ScheduleErrors.buildingNotFound(buildingId));
        if (number == null || number.isBlank()) {
            throw Errors.badRequest("Room number is required");
        }
        if (capacity != null && capacity < 0) {
            throw Errors.badRequest("Room capacity must be >= 0");
        }
        Room entity = Room.builder()
                .building(building)
                .number(number.trim())
                .capacity(capacity)
                .type(type != null ? type.trim() : null)
                .build();
        return ScheduleMappers.toRoomDto(roomRepository.save(entity));
    }

    @Transactional
    RoomDto update(UUID id, UUID buildingId, String number, Integer capacity, String type) {
        Room entity = roomRepository.findById(id)
                .orElseThrow(() -> ScheduleErrors.roomNotFound(id));
        if (buildingId != null) {
            Building building = buildingRepository.findById(buildingId)
                    .orElseThrow(() -> ScheduleErrors.buildingNotFound(buildingId));
            entity.setBuilding(building);
        }
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
    List<RoomDto> createBulk(List<RoomBulkCreateItem> items) {
        List<RoomDto> result = new java.util.ArrayList<>(items.size());
        for (RoomBulkCreateItem item : items) {
            result.add(create(item.buildingId(), item.number(), item.capacity(), item.type()));
        }
        return result;
    }

    @Transactional
    void delete(UUID id) {
        if (!roomRepository.existsById(id)) {
            throw ScheduleErrors.roomNotFound(id);
        }
        roomRepository.deleteById(id);
    }

    /**
     * Single room item for bulk create. Internal use only.
     */
    record RoomBulkCreateItem(UUID buildingId, String number, Integer capacity, String type) {}
}
