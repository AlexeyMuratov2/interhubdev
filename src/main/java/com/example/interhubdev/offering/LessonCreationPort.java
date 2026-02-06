package com.example.interhubdev.offering;

import java.time.LocalDate;
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
     * Command to create a single lesson.
     *
     * @param offeringId offering ID
     * @param date lesson date
     * @param timeslotId timeslot ID
     * @param roomId room ID (nullable)
     * @param status lesson status (e.g., "planned")
     */
    record LessonCreateCommand(
            UUID offeringId,
            LocalDate date,
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
     *
     * @param offeringId offering ID
     */
    void deleteLessonsByOfferingId(UUID offeringId);
}
