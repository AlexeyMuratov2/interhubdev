package com.example.interhubdev.schedule;

import java.util.List;
import java.util.UUID;

/**
 * Port for student lookup. Used by Schedule to resolve group IDs for the current student
 * so that lessons for the week can be returned without depending on the Student module.
 * Implemented by an adapter in the adapter package.
 */
public interface StudentLookupPort {

    /**
     * Check if the user has a student profile.
     *
     * @param userId user ID
     * @return true if user has a student profile
     */
    boolean hasStudentProfile(UUID userId);

    /**
     * Get group IDs the student (user) belongs to. Call only when {@link #hasStudentProfile(UUID)} is true.
     *
     * @param userId user ID of the student
     * @return list of group UUIDs; empty if student has no group memberships
     */
    List<UUID> getGroupIdsByUserId(UUID userId);
}
