package com.example.interhubdev.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Schedule module: rooms, timeslots, lessons.
 */
public interface ScheduleApi {

    // --- Room ---
    Optional<RoomDto> findRoomById(UUID id);

    List<RoomDto> findAllRooms();

    RoomDto createRoom(String building, String number, Integer capacity, String type);

    RoomDto updateRoom(UUID id, String building, String number, Integer capacity, String type);

    void deleteRoom(UUID id);

    // --- Timeslot ---
    Optional<TimeslotDto> findTimeslotById(UUID id);

    List<TimeslotDto> findAllTimeslots();

    TimeslotDto createTimeslot(int dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime);

    void deleteTimeslot(UUID id);

    // --- Lesson ---
    Optional<LessonDto> findLessonById(UUID id);

    List<LessonDto> findLessonsByOfferingId(UUID offeringId);

    List<LessonDto> findLessonsByDate(LocalDate date);

    LessonDto createLesson(UUID offeringId, LocalDate date, UUID timeslotId, UUID roomId, String topic, String status);

    LessonDto updateLesson(UUID id, UUID roomId, String topic, String status);

    void deleteLesson(UUID id);
}
