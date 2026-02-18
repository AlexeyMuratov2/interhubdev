/**
 * Schedule module - buildings, rooms, timeslots, and lessons.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.schedule.ScheduleApi} - buildings, rooms, timeslots, lessons (facade)</li>
 *   <li>{@link com.example.interhubdev.schedule.BuildingDto}, {@link com.example.interhubdev.schedule.RoomDto},
 *       {@link com.example.interhubdev.schedule.TimeslotDto}, {@link com.example.interhubdev.schedule.LessonDto} - DTOs</li>
 *   <li>{@link com.example.interhubdev.schedule.LessonForScheduleDto}, {@link com.example.interhubdev.schedule.OfferingSummaryDto},
 *       {@link com.example.interhubdev.schedule.SlotSummaryDto}, {@link com.example.interhubdev.schedule.TeacherRoleDto},
 *       {@link com.example.interhubdev.schedule.RoomSummaryDto}, {@link com.example.interhubdev.schedule.TeacherSummaryDto},
 *       {@link com.example.interhubdev.schedule.GroupSummaryDto} - schedule display DTOs</li>
 *   <li>{@link com.example.interhubdev.schedule.RoomCreateRequest} - request for single or bulk room creation</li>
 *   <li>{@link com.example.interhubdev.schedule.TimeslotCreateRequest} - request for single or bulk timeslot creation</li>
 *   <li>{@link com.example.interhubdev.schedule.LessonBulkCreateRequest} - request item for bulk lesson creation</li>
 *   <li>{@link com.example.interhubdev.schedule.OfferingLookupPort}, {@link com.example.interhubdev.schedule.GroupLookupPort},
 *       {@link com.example.interhubdev.schedule.LessonEnrichmentPort}, {@link com.example.interhubdev.schedule.TeacherLookupPort},
 *       {@link com.example.interhubdev.schedule.RoomExistsPort} - ports</li>
 * </ul>
 *
 * <h2>Internal structure</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleServiceImpl} - facade implementing ScheduleApi</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleBuildingService} - CRUD for buildings</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleRoomService} - CRUD for rooms (references building)</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleTimeslotService} - CRUD for timeslots</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleLessonService} - CRUD for lessons (validates offering)</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleMappers} - entity to DTO mapping</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleValidation} - date/time parsing, lesson status</li>
 *   <li>{@link com.example.interhubdev.schedule.internal.ScheduleErrors} - module error codes and factory</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Write operations (create/update/delete) only for roles: MODERATOR, ADMIN, SUPER_ADMIN. Read operations for all authenticated users.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors} or {@link com.example.interhubdev.schedule.internal.ScheduleErrors}</li>
 *   <li>offering is used via port {@link com.example.interhubdev.schedule.OfferingLookupPort} (adapter in adapter package)</li>
 * </ul>
 *
 * <h2>Error codes (via {@link com.example.interhubdev.schedule.internal.ScheduleErrors} or {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>SCHEDULE_BUILDING_NOT_FOUND, SCHEDULE_ROOM_NOT_FOUND, SCHEDULE_TIMESLOT_NOT_FOUND, SCHEDULE_LESSON_NOT_FOUND, SCHEDULE_OFFERING_NOT_FOUND, SCHEDULE_GROUP_NOT_FOUND (404)</li>
 *   <li>SCHEDULE_BUILDING_HAS_ROOMS, SCHEDULE_LESSON_ALREADY_EXISTS (409)</li>
 *   <li>SCHEDULE_TEACHER_PROFILE_NOT_FOUND (403) - user does not have a teacher profile</li>
 *   <li>BAD_REQUEST (400) - building name/room number required; capacity &lt; 0; dayOfWeek not 1..7; invalid date/time format; status not PLANNED/CANCELLED/DONE; endTime not after startTime</li>
 *   <li>VALIDATION_FAILED (400) - request validation failed (@Valid on create/update)</li>
 *   <li>FORBIDDEN (403) - user has no MODERATOR/ADMIN/SUPER_ADMIN role for write operations</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Schedule",
    allowedDependencies = {"error"}
)
package com.example.interhubdev.schedule;
