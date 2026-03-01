package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gateway for roster (group membership) information.
 */
final class RosterGateway {

    private final StudentApi studentApi;

    RosterGateway(StudentApi studentApi) {
        this.studentApi = studentApi;
    }

    Set<UUID> getStudentIdsByGroupId(UUID groupId) {
        return studentApi.findByGroupId(groupId).stream()
                .map(StudentDto::id)
                .collect(Collectors.toSet());
    }

    boolean isStudentInGroup(UUID studentId, UUID groupId) {
        return getStudentIdsByGroupId(groupId).contains(studentId);
    }
}
