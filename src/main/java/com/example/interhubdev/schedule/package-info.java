/**
 * Schedule module - rooms, timeslots, and lessons.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.schedule.ScheduleApi} - rooms, timeslots, lessons</li>
 *   <li>{@link com.example.interhubdev.schedule.RoomDto}, {@link com.example.interhubdev.schedule.TimeslotDto},
 *       {@link com.example.interhubdev.schedule.LessonDto} - DTOs</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Create, update and delete operations for rooms, timeslots and lessons are allowed
 * only for roles: STAFF, ADMIN, SUPER_ADMIN. Teachers and students can only read.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 * </ul>
 *
 * <h2>Error codes (via {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>NOT_FOUND (404) - room, timeslot or lesson not found</li>
 *   <li>BAD_REQUEST (400) - building/number required; capacity &lt; 0; dayOfWeek not 1..7; startTime/endTime required or invalid; endTime not after startTime; offering/date/timeslot required; invalid date/time format; status not planned/cancelled/done</li>
 *   <li>VALIDATION_FAILED (400) - request validation failed (@Valid on create/update)</li>
 *   <li>FORBIDDEN (403) - user has no STAFF/ADMIN/SUPER_ADMIN role for write operations</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Schedule",
    allowedDependencies = {"error"}
)
package com.example.interhubdev.schedule;
