package com.example.interhubdev.group;

import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.user.UserDto;

/**
 * Group member response: student profile and linked user (for display name, email, etc.).
 * Returned by GET /api/groups/{groupId}/members.
 */
public record GroupMemberDto(
        StudentDto student,
        UserDto user
) {
}
