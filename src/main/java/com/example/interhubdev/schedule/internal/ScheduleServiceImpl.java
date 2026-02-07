package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.schedule.BuildingDto;
import com.example.interhubdev.schedule.LessonBulkCreateRequest;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.LessonForScheduleDto;
import com.example.interhubdev.schedule.RoomCreateRequest;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.schedule.TimeslotCreateRequest;
import com.example.interhubdev.schedule.TimeslotDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements ScheduleApi; orchestrates building, room, timeslot, lesson services. */
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
    public TimeslotDto createTimeslot(TimeslotCreateRequest request) {
        var startTime = ScheduleValidation.parseTime(request.startTime(), "startTime");
        var endTime = ScheduleValidation.parseTime(request.endTime(), "endTime");
        return timeslotService.create(request.dayOfWeek(), startTime, endTime);
    }

    @Override
    @Transactional
    public List<TimeslotDto> createTimeslotsInBulk(List<TimeslotCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<ScheduleTimeslotService.TimeslotBulkItem> items = requests.stream()
                .map(r -> new ScheduleTimeslotService.TimeslotBulkItem(
                        r.dayOfWeek(),
                        ScheduleValidation.parseTime(r.startTime(), "startTime"),
                        ScheduleValidation.parseTime(r.endTime(), "endTime")))
                .toList();
        return timeslotService.createBulk(items);
    }

    @Override
    @Transactional
    public void deleteTimeslot(UUID id) {
        timeslotService.delete(id);
    }

    @Override
    @Transactional
    public void deleteAllTimeslots() {
        timeslotService.deleteAll();
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
    public List<LessonForScheduleDto> findLessonsByDate(LocalDate date) {
        return lessonService.findByDateEnriched(date);
    }

    @Override
    public List<LessonForScheduleDto> findLessonsByWeek(LocalDate date) {
        return lessonService.findByWeekEnriched(date);
    }

    @Override
    public List<LessonForScheduleDto> findLessonsByWeekAndGroupId(LocalDate date, UUID groupId) {
        return lessonService.findByWeekAndGroupIdEnriched(date, groupId);
    }

    @Override
    public List<LessonForScheduleDto> findLessonsByDateAndGroupId(LocalDate date, UUID groupId) {
        return lessonService.findByDateAndGroupIdEnriched(date, groupId);
    }

    @Override
    @Transactional
    public LessonDto createLesson(UUID offeringId, String date, String startTime, String endTime,
                                  UUID timeslotId, UUID roomId, String topic, String status) {
        return lessonService.create(offeringId, date, startTime, endTime, timeslotId, roomId, topic, status);
    }

    @Override
    @Transactional
    public LessonDto updateLesson(UUID id, String startTime, String endTime, UUID roomId, String topic, String status) {
        return lessonService.update(id, startTime, endTime, roomId, topic, status);
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

    @Override
    @Transactional
    public void deleteLessonsByOfferingIdAndDateRange(UUID offeringId, LocalDate startInclusive, LocalDate endInclusive) {
        lessonService.deleteByOfferingIdAndDateBetween(offeringId, startInclusive, endInclusive);
    }

    @Override
    @Transactional
    public void deleteLessonsByOfferingSlotId(UUID offeringSlotId) {
        lessonService.deleteByOfferingSlotId(offeringSlotId);
    }

    @Override
    @Transactional
    public void deleteLessonsByOfferingIdAndDayOfWeekAndStartTimeAndEndTime(
            UUID offeringId, int dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime) {
        lessonService.deleteByOfferingIdAndDayOfWeekAndStartTimeAndEndTime(
                offeringId, dayOfWeek, startTime, endTime);
    }
}
