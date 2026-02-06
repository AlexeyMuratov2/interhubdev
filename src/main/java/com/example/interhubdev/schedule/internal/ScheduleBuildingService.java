package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.schedule.BuildingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD for buildings. Delete is allowed only when building has no rooms.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ScheduleBuildingService {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;

    Optional<BuildingDto> findById(UUID id) {
        return buildingRepository.findById(id).map(ScheduleMappers::toBuildingDto);
    }

    List<BuildingDto> findAll() {
        return buildingRepository.findAllByOrderByNameAsc().stream()
                .map(ScheduleMappers::toBuildingDto)
                .toList();
    }

    @Transactional
    BuildingDto create(String name, String address) {
        if (name == null || name.isBlank()) {
            throw Errors.badRequest("Building name is required");
        }
        Building entity = Building.builder()
                .name(name.trim())
                .address(address != null ? address.trim() : null)
                .build();
        return ScheduleMappers.toBuildingDto(buildingRepository.save(entity));
    }

    @Transactional
    BuildingDto update(UUID id, String name, String address) {
        Building entity = buildingRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Building not found: " + id));
        if (name != null) entity.setName(name.trim());
        if (address != null) entity.setAddress(address.trim());
        entity.setUpdatedAt(LocalDateTime.now());
        return ScheduleMappers.toBuildingDto(buildingRepository.save(entity));
    }

    @Transactional
    void delete(UUID id) {
        if (!buildingRepository.existsById(id)) {
            throw Errors.notFound("Building not found: " + id);
        }
        long roomCount = roomRepository.countByBuildingId(id);
        if (roomCount > 0) {
            throw Errors.conflict("Building has rooms; delete or reassign rooms first");
        }
        buildingRepository.deleteById(id);
    }
}
