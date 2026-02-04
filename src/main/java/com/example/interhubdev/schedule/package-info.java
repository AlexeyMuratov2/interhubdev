/**
 * Schedule module - rooms, timeslots, and lessons.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.schedule.ScheduleApi} - rooms, timeslots, lessons (facade)</li>
 *   <li>{@link com.example.interhubdev.schedule.RoomDto}, {@link com.example.interhubdev.schedule.TimeslotDto},
 *       {@link com.example.interhubdev.schedule.LessonDto} - DTOs</li>
 * </ul>
 *
 * <h2>Internal structure</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleServiceImpl} - facade implementing ScheduleApi</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleRoomService} - CRUD for rooms</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleTimeslotService} - CRUD for timeslots</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleLessonService} - CRUD for lessons (validates offering)</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleMappers} - entity to DTO mapping</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleValidation} - lesson status normalization</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Write operations (create/update/delete) only for roles: MODERATOR, ADMIN, SUPER_ADMIN. Read operations for all authenticated users.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>offering - lessons reference offering; offering existence validated on lesson create</li>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 * </ul>
 *
 * <h2>Error codes (via {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>NOT_FOUND (404) - room, timeslot, lesson or offering not found</li>
 *   <li>CONFLICT (409) - lesson already exists for same offering, date and timeslot</li>
 *   <li>BAD_REQUEST (400) - building/number required; capacity &lt; 0; dayOfWeek not 1..7; startTime/endTime required or invalid; endTime not after startTime; offering/date/timeslot required; invalid date/time format; status not planned/cancelled/done</li>
 *   <li>VALIDATION_FAILED (400) - request validation failed (@Valid on create/update)</li>
 *   <li>FORBIDDEN (403) - user has no MODERATOR/ADMIN/SUPER_ADMIN role for write operations</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Schedule",
    allowedDependencies = {"offering", "error"}
)
package com.example.interhubdev.schedule;
