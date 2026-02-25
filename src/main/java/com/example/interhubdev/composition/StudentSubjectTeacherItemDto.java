package com.example.interhubdev.composition;

import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.UserDto;

/**
 * One teacher assigned to an offering, with full profile and user display data.
 * Wrapper aggregating TeacherDto + UserDto + role for the student subject detail screen.
 */
public record StudentSubjectTeacherItemDto(
    TeacherDto teacher,
    UserDto user,
    /** Role in the offering: null or "MAIN" for the main teacher; "LECTURE", "PRACTICE", "LAB" for slot teachers. */
    String role
) {
}
