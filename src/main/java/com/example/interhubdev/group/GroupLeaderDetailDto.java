package com.example.interhubdev.group;

import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.user.UserDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Group leader with full student and user data for display.
 * Returned by GET /api/groups/{groupId}/leaders.
 */
public record GroupLeaderDetailDto(
        UUID id,
        UUID groupId,
        UUID studentId,
        String role,
        LocalDate fromDate,
        LocalDate toDate,
        LocalDateTime createdAt,
        StudentDto student,
        UserDto user
) {
}
