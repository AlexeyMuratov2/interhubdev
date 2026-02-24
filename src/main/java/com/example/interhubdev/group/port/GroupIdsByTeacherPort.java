package com.example.interhubdev.group.port;

import java.util.List;
import java.util.UUID;

/**
 * Port for resolving group IDs by teacher ID (groups where the teacher has at least one lesson).
 * Implemented by an adapter that uses Offering and Schedule modules to compute the list.
 * Allows Group module to expose "groups by teacher" without depending on Offering/Schedule.
 */
public interface GroupIdsByTeacherPort {

    /**
     * Get IDs of student groups where the given teacher has at least one offering slot with at least one lesson.
     *
     * @param teacherId teacher entity ID
     * @return list of group IDs (may be empty)
     */
    List<UUID> findGroupIdsByTeacherId(UUID teacherId);
}
