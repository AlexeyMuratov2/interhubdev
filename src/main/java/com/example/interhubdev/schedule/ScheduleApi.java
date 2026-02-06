package com.example.interhubdev.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Schedule module: buildings, rooms, timeslots, lessons.
 */
public interface ScheduleApi {

    // --- Building ---
    Optional<BuildingDto> findBuildingById(UUID id);

    List<BuildingDto> findAllBuildings();

    BuildingDto createBuilding(String name, String address);

    BuildingDto updateBuilding(UUID id, String name, String address);

    /**
     * Delete building. Fails with CONFLICT if building has any rooms.
     *
     * @param id building ID
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if building missing, CONFLICT if building has rooms (via Errors)
     */
    void deleteBuilding(UUID id);

    // --- Room ---
    Optional<RoomDto> findRoomById(UUID id);

    List<RoomDto> findAllRooms();

    RoomDto createRoom(UUID buildingId, String number, Integer capacity, String type);

    /**
     * Create multiple rooms in one transaction. If any item fails validation or building is not found, the whole batch fails.
     *
     * @param requests list of room creation items (buildingId, number, capacity, type)
     * @return list of created room DTOs in the same order as requests
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST on validation, NOT_FOUND if building missing (via Errors)
     */
    List<RoomDto> createRoomsInBulk(List<RoomCreateRequest> requests);

    RoomDto updateRoom(UUID id, UUID buildingId, String number, Integer capacity, String type);

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

    /**
     * Create multiple lessons in a single batch. Skips individual validation per lesson
     * (offering/timeslot existence assumed validated by caller). Duplicates are silently skipped.
     *
     * @param requests list of lesson creation requests
     * @return list of created lesson DTOs
     */
    List<LessonDto> createLessonsInBulk(List<LessonBulkCreateRequest> requests);

    /**
     * Delete all lessons belonging to a specific offering.
     *
     * @param offeringId offering ID
     */
    void deleteLessonsByOfferingId(UUID offeringId);
}
