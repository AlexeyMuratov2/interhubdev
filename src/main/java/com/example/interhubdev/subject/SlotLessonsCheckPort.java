package com.example.interhubdev.subject;

import java.util.Collection;
import java.util.UUID;

/**
 * Port to check whether offerings have at least one lesson (by slot).
 * Implemented by adapter using offering and schedule modules.
 */
public interface SlotLessonsCheckPort {

    /**
     * Check if at least one of the given offerings has at least one lesson in its slots.
     *
     * @param offeringIds offering IDs to check (empty collection returns false)
     * @return true if there exists at least one offering slot with at least one lesson
     */
    boolean hasAtLeastOneLessonForOfferings(Collection<UUID> offeringIds);
}
