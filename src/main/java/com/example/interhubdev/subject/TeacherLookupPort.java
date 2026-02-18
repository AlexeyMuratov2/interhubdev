package com.example.interhubdev.subject;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for teacher lookup by user ID.
 * Implemented by adapter to avoid direct dependency on teacher module.
 */
public interface TeacherLookupPort {

    /**
     * Get teacher entity ID by user ID.
     *
     * @param userId user ID
     * @return teacher entity ID if user has a teacher profile
     */
    Optional<UUID> getTeacherIdByUserId(UUID userId);
}
