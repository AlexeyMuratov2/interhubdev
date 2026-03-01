package com.example.interhubdev.attendancerecord.internal;

import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;

import java.util.List;
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

    List<StudentDto> getRosterByGroupId(UUID groupId) {
        return studentApi.findByGroupId(groupId);
    }

    Set<UUID> getStudentIdsByGroupId(UUID groupId) {
        return studentApi.findByGroupId(groupId).stream()
                .map(StudentDto::id)
                .collect(Collectors.toSet());
    }
}
