package com.example.interhubdev.offering;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Port for creating and deleting lessons in the Schedule module.
 * Used by the Offering module's lesson generation logic.
 * <p>
 * Implemented by an adapter in the adapter package using ScheduleApi.
 */
public interface LessonCreationPort {

    /**
     * Command to create a single lesson. Lesson owns time (startTime, endTime).
     * timeslotId is optional (UI hint).
     */
    record LessonCreateCommand(
            UUID offeringId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            UUID timeslotId,
            UUID roomId,
            String status
    ) {}

    /**
     * Create multiple lessons in a single batch operation.
     *
     * @param commands list of lesson creation commands
     * @return number of lessons actually created
     */
    int createLessonsInBulk(List<LessonCreateCommand> commands);

    /**
     * Delete all lessons belonging to a specific offering.
     */
    void deleteLessonsByOfferingId(UUID offeringId);
}
