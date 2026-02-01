/**
 * Schedule module - rooms, timeslots, and lessons.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.schedule.ScheduleApi} - rooms, timeslots, lessons</li>
 *   <li>{@link com.example.interhubdev.schedule.RoomDto}, {@link com.example.interhubdev.schedule.TimeslotDto} - DTOs</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Schedule",
    allowedDependencies = {}
)
package com.example.interhubdev.schedule;
