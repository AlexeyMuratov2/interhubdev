package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gateway for accessing roster (student group membership) information.
 * Abstracts StudentApi calls for attendance module.
 */
final class RosterGateway {

    private final StudentApi studentApi;

    RosterGateway(StudentApi studentApi) {
        this.studentApi = studentApi;
    }

    /**
     * Get roster (list of students) for a group.
     *
     * @param groupId group ID
     * @return list of student DTOs in the group
     */
    List<StudentDto> getRosterByGroupId(UUID groupId) {
        return studentApi.findByGroupId(groupId);
    }

    /**
     * Get set of student IDs in a group.
     *
     * @param groupId group ID
     * @return set of student profile IDs
     */
    Set<UUID> getStudentIdsByGroupId(UUID groupId) {
        return studentApi.findByGroupId(groupId).stream()
                .map(StudentDto::id)
                .collect(Collectors.toSet());
    }

    /**
     * Check if student is in group roster.
     *
     * @param studentId student profile ID
     * @param groupId   group ID
     * @return true if student is in group
     */
    boolean isStudentInGroup(UUID studentId, UUID groupId) {
        Set<UUID> studentIds = getStudentIdsByGroupId(groupId);
        return studentIds.contains(studentId);
    }
}
