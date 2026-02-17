package com.example.interhubdev.document;

import java.util.UUID;

/**
 * Port for checking lesson existence without depending on schedule module.
 * Implemented by adapter that delegates to {@link com.example.interhubdev.schedule.ScheduleApi}.
 */
public interface LessonLookupPort {

    /**
     * Check if a lesson exists by id.
     *
     * @param lessonId lesson UUID
     * @return true if lesson exists
     */
    boolean existsById(UUID lessonId);
}
