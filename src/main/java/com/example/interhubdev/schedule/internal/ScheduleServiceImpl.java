package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.schedule.*;
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

    private final ScheduleRoomService roomService;
    private final ScheduleTimeslotService timeslotService;
    private final ScheduleLessonService lessonService;

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
    public RoomDto createRoom(String building, String number, Integer capacity, String type) {
        return roomService.create(building, number, capacity, type);
    }

    @Override
    @Transactional
    public RoomDto updateRoom(UUID id, String building, String number, Integer capacity, String type) {
        return roomService.update(id, building, number, capacity, type);
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
}
