package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.schedule.BuildingDto;
import com.example.interhubdev.schedule.LessonBulkCreateRequest;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.RoomCreateRequest;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.schedule.TimeslotDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ScheduleServiceImpl implements ScheduleApi {

    private final ScheduleBuildingService buildingService;
    private final ScheduleRoomService roomService;
    private final ScheduleTimeslotService timeslotService;
    private final ScheduleLessonService lessonService;

    @Override
    public Optional<BuildingDto> findBuildingById(UUID id) {
        return buildingService.findById(id);
    }

    @Override
    public List<BuildingDto> findAllBuildings() {
        return buildingService.findAll();
    }

    @Override
    @Transactional
    public BuildingDto createBuilding(String name, String address) {
        return buildingService.create(name, address);
    }

    @Override
    @Transactional
    public BuildingDto updateBuilding(UUID id, String name, String address) {
        return buildingService.update(id, name, address);
    }

    @Override
    @Transactional
    public void deleteBuilding(UUID id) {
        buildingService.delete(id);
    }

    @Override
    public Optional<RoomDto> findRoomById(UUID id) {
        return roomService.findById(id);
    }

    @Override
    public List<RoomDto> findAllRooms() {
        return roomService.findAll();
    }

    @Override
    @Transactional
    public RoomDto createRoom(UUID buildingId, String number, Integer capacity, String type) {
        return roomService.create(buildingId, number, capacity, type);
    }

    @Override
    @Transactional
    public List<RoomDto> createRoomsInBulk(List<RoomCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<ScheduleRoomService.RoomBulkCreateItem> items = requests.stream()
                .map(r -> new ScheduleRoomService.RoomBulkCreateItem(r.buildingId(), r.number(), r.capacity(), r.type()))
                .toList();
        return roomService.createBulk(items);
    }

    @Override
    @Transactional
    public RoomDto updateRoom(UUID id, UUID buildingId, String number, Integer capacity, String type) {
        return roomService.update(id, buildingId, number, capacity, type);
    }

    @Override
    @Transactional
    public void deleteRoom(UUID id) {
        roomService.delete(id);
    }

    @Override
    public Optional<TimeslotDto> findTimeslotById(UUID id) {
        return timeslotService.findById(id);
    }

    @Override
    public List<TimeslotDto> findAllTimeslots() {
        return timeslotService.findAll();
    }

    @Override
    @Transactional
    public TimeslotDto createTimeslot(int dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime) {
        return timeslotService.create(dayOfWeek, startTime, endTime);
    }

    @Override
    @Transactional
    public void deleteTimeslot(UUID id) {
        timeslotService.delete(id);
    }

    @Override
    public Optional<LessonDto> findLessonById(UUID id) {
        return lessonService.findById(id);
    }

    @Override
    public List<LessonDto> findLessonsByOfferingId(UUID offeringId) {
        return lessonService.findByOfferingId(offeringId);
    }

    @Override
    public List<LessonDto> findLessonsByDate(LocalDate date) {
        return lessonService.findByDate(date);
    }

    @Override
    @Transactional
    public LessonDto createLesson(UUID offeringId, LocalDate date, UUID timeslotId, UUID roomId, String topic, String status) {
        return lessonService.create(offeringId, date, timeslotId, roomId, topic, status);
    }

    @Override
    @Transactional
    public LessonDto updateLesson(UUID id, UUID roomId, String topic, String status) {
        return lessonService.update(id, roomId, topic, status);
    }

    @Override
    @Transactional
    public void deleteLesson(UUID id) {
        lessonService.delete(id);
    }

    @Override
    @Transactional
    public List<LessonDto> createLessonsInBulk(List<LessonBulkCreateRequest> requests) {
        return lessonService.createBulk(requests);
    }

    @Override
    @Transactional
    public void deleteLessonsByOfferingId(UUID offeringId) {
        lessonService.deleteByOfferingId(offeringId);
    }
}
