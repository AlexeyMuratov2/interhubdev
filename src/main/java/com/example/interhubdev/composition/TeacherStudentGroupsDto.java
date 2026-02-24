package com.example.interhubdev.composition;

import java.util.List;

/**
 * Response for GET /api/composition/teacher/student-groups.
 * Lists all groups where the current teacher has at least one lesson (slots with lessons only).
 */
public record TeacherStudentGroupsDto(
    List<TeacherStudentGroupItemDto> groups
) {
}
