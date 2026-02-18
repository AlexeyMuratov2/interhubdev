package com.example.interhubdev.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Schedule module: buildings, rooms, timeslots (time templates), lessons.
 * All errors are thrown as {@link com.example.interhubdev.error.AppException} (via Errors or ScheduleErrors),
 * handled by global exception handler as ErrorResponse.
 */
public interface ScheduleApi {

    // --- Building ---

    /**
     * Find building by ID.
     *
     * @param id building ID
     * @return optional building DTO if found; empty if not found (controller throws ScheduleErrors.buildingNotFound for 404)
     */
    Optional<BuildingDto> findBuildingById(UUID id);

    /**
     * List all buildings ordered by name.
     *
     * @return list of building DTOs
     */
    List<BuildingDto> findAllBuildings();

    /**
     * Create a building.
     *
     * @param name    building name (required, non-blank)
     * @param address optional address
     * @return created building DTO
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if name blank
     */
    BuildingDto createBuilding(String name, String address);

    /**
     * Update a building.
     *
     * @param id      building ID
     * @param name    new name (optional)
     * @param address new address (optional)
     * @return updated building DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if building not found (ScheduleErrors.buildingNotFound)
     */
    BuildingDto updateBuilding(UUID id, String name, String address);

    /**
     * Delete a building. Fails if building has rooms.
     *
     * @param id building ID
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if not found, CONFLICT if building has rooms (ScheduleErrors)
     */
    void deleteBuilding(UUID id);

    // --- Room ---

    /**
     * Find room by ID.
     *
     * @param id room ID
     * @return optional room DTO if found
     */
    Optional<RoomDto> findRoomById(UUID id);

    /**
     * List all rooms (e.g. ordered by building name, room number).
     *
     * @return list of room DTOs
     */
    List<RoomDto> findAllRooms();

    /**
     * Create a room in a building.
     *
     * @param buildingId building ID (required)
     * @param number     room number (required)
     * @param capacity   capacity (optional, must be &gt;= 0)
     * @param type       optional type
     * @return created room DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if building not found, BAD_REQUEST if number blank or capacity &lt; 0
     */
    RoomDto createRoom(UUID buildingId, String number, Integer capacity, String type);

    /**
     * Create multiple rooms in one transaction.
     *
     * @param requests list of room create requests
     * @return list of created room DTOs
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if any building not found, BAD_REQUEST on validation
     */
    List<RoomDto> createRoomsInBulk(List<RoomCreateRequest> requests);

    /**
     * Update a room.
     *
     * @param id         room ID
     * @param buildingId optional new building ID
     * @param number     optional new number
     * @param capacity   optional new capacity (&gt;= 0)
     * @param type       optional new type
     * @return updated room DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if room or building not found, BAD_REQUEST if capacity &lt; 0
     */
    RoomDto updateRoom(UUID id, UUID buildingId, String number, Integer capacity, String type);

    /**
     * Delete a room.
     *
     * @param id room ID
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if room not found
     */
    void deleteRoom(UUID id);

    // --- Timeslot (time templates for UI) ---

    /**
     * Find timeslot by ID.
     *
     * @param id timeslot ID
     * @return optional timeslot DTO if found
     */
    Optional<TimeslotDto> findTimeslotById(UUID id);

    /**
     * List all timeslots (e.g. ordered by dayOfWeek, startTime).
     *
     * @return list of timeslot DTOs
     */
    List<TimeslotDto> findAllTimeslots();

    /**
     * Create a timeslot. startTime/endTime parsed from strings (HH:mm or HH:mm:ss).
     *
     * @param request timeslot create request (dayOfWeek 1..7, startTime, endTime required)
     * @return created timeslot DTO
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if dayOfWeek invalid, time format invalid, or endTime not after startTime
     */
    TimeslotDto createTimeslot(TimeslotCreateRequest request);

    /**
     * Create multiple timeslots in one transaction.
     *
     * @param requests list of timeslot create requests
     * @return list of created timeslot DTOs
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST on validation
     */
    List<TimeslotDto> createTimeslotsInBulk(List<TimeslotCreateRequest> requests);

    /**
     * Delete a timeslot. Lessons that referenced this timeslot are not deleted; their timeslotId is set to null.
     *
     * @param id timeslot ID
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if timeslot not found
     */
    void deleteTimeslot(UUID id);

    /**
     * Delete all timeslots. Lessons that referenced any timeslot are not deleted; their timeslotId is set to null.
     */
    void deleteAllTimeslots();

    // --- Lesson (owns date and time) ---

    /**
     * Find lesson by ID.
     *
     * @param id lesson ID
     * @return optional lesson DTO if found
     */
    Optional<LessonDto> findLessonById(UUID id);

    /**
     * List lessons by offering ID (ordered by date, startTime).
     *
     * @param offeringId offering ID
     * @return list of lesson DTOs
     */
    List<LessonDto> findLessonsByOfferingId(UUID offeringId);

    /**
     * List lessons by date with full context for schedule UI (offering, slot, teachers). Ordered by startTime.
     *
     * @param date date
     * @return list of lesson with offering summary, slot summary and teachers (batch-loaded, no N+1)
     */
    List<LessonForScheduleDto> findLessonsByDate(LocalDate date);

    /**
     * List lessons for the week containing the given date (ISO week: Monday–Sunday) with full context (offering, slot, teachers).
     * Same structure as findLessonsByDate; ordered by date then startTime. Batch-loaded, no N+1.
     *
     * @param date any date in the week (used to compute week bounds)
     * @return list of lesson with offering summary, slot summary and teachers for the whole week
     */
    List<LessonForScheduleDto> findLessonsByWeek(LocalDate date);

    /**
     * List lessons for the week containing the given date for the given group, with full context (offering, slot, teachers).
     * Same structure as findLessonsByWeek; ordered by date then startTime. Returns empty list if group has no offerings or no lessons in the week.
     *
     * @param date    any date in the week (used to compute week bounds)
     * @param groupId group ID (404 if group does not exist)
     * @return list of lesson with offering summary, slot summary and teachers for the group's week
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if group does not exist (ScheduleErrors.groupNotFound)
     */
    List<LessonForScheduleDto> findLessonsByWeekAndGroupId(LocalDate date, UUID groupId);

    /**
     * List lessons on the given date for the given group with full context (offering, slot, teachers). Ordered by startTime.
     * Returns empty list if group has no offerings or does not exist.
     *
     * @param date    date
     * @param groupId group ID
     * @return list of lesson with offering summary, slot summary and teachers (batch-loaded, no N+1)
     */
    List<LessonForScheduleDto> findLessonsByDateAndGroupId(LocalDate date, UUID groupId);

    /**
     * List lessons for the week containing the given date for the current authenticated teacher, with full context (offering, slot, teachers, group).
     * Same structure as findLessonsByWeek; ordered by date then startTime. Returns empty list if teacher has no offerings or no lessons in the week.
     *
     * @param date any date in the week (used to compute week bounds)
     * @param teacherId teacher entity ID
     * @return list of lesson with offering summary, slot summary, teachers and group for the teacher's week
     */
    List<LessonForScheduleDto> findLessonsByWeekAndTeacherId(LocalDate date, UUID teacherId);

    /**
     * Create a lesson. Date and times are passed as strings and parsed (date: yyyy-MM-dd, time: HH:mm or HH:mm:ss).
     *
     * @param offeringId offering ID (required)
     * @param date       date string ISO-8601 (required)
     * @param startTime  start time string (required)
     * @param endTime    end time string (required, must be after startTime)
     * @param timeslotId optional timeslot ID (UI hint)
     * @param roomId     optional room ID
     * @param topic      optional topic
     * @param status     optional status (PLANNED/CANCELLED/DONE, default PLANNED)
     * @return created lesson DTO
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if date/time invalid or endTime not after startTime; NOT_FOUND if offering/timeslot/room not found; CONFLICT if lesson already exists for same offering+date+time
     */
    LessonDto createLesson(UUID offeringId, String date, String startTime, String endTime,
                          UUID timeslotId, UUID roomId, String topic, String status);

    /**
     * Update a lesson. startTime/endTime optional; if both provided, parsed and validated (endTime after startTime).
     *
     * @param id        lesson ID
     * @param startTime optional start time string (HH:mm or HH:mm:ss)
     * @param endTime   optional end time string
     * @param roomId    optional room ID
     * @param topic     optional topic
     * @param status    optional status (PLANNED/CANCELLED/DONE)
     * @return updated lesson DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if lesson or room not found; BAD_REQUEST if time format invalid or endTime not after startTime
     */
    LessonDto updateLesson(UUID id, String startTime, String endTime, UUID roomId, String topic, String status);

    /**
     * Delete a lesson.
     *
     * @param id lesson ID
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if lesson not found
     */
    void deleteLesson(UUID id);

    /**
     * Create multiple lessons in one transaction. Duplicates (same offering+date+start+end) are skipped.
     *
     * @param requests list of bulk create requests (with LocalDate/LocalTime)
     * @return list of created lesson DTOs
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if any offering/timeslot/room not found; BAD_REQUEST on validation
     */
    List<LessonDto> createLessonsInBulk(List<LessonBulkCreateRequest> requests);

    /**
     * Delete all lessons for an offering.
     *
     * @param offeringId offering ID
     */
    void deleteLessonsByOfferingId(UUID offeringId);

    /**
     * Delete lessons for an offering whose date is within the given range (inclusive).
     * Used when regenerating lessons for a single semester so lessons of other semesters are preserved.
     *
     * @param offeringId offering ID
     * @param startInclusive start date (inclusive)
     * @param endInclusive end date (inclusive)
     */
    void deleteLessonsByOfferingIdAndDateRange(UUID offeringId, java.time.LocalDate startInclusive, java.time.LocalDate endInclusive);

    /**
     * Delete all lessons that reference the given offering slot (generated from that slot).
     * Used when an offering slot is removed.
     *
     * @param offeringSlotId offering slot ID
     */
    void deleteLessonsByOfferingSlotId(UUID offeringSlotId);

    /**
     * Delete lessons for an offering that match the given weekly slot (day of week and time).
     * Used when an offering slot is removed (for legacy lessons without offeringSlotId).
     *
     * @param offeringId offering ID
     * @param dayOfWeek  day of week (1–7, Monday=1)
     * @param startTime  slot start time
     * @param endTime    slot end time
     */
    void deleteLessonsByOfferingIdAndDayOfWeekAndStartTimeAndEndTime(
            UUID offeringId, int dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime);
}
