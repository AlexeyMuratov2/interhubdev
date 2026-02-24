package com.example.interhubdev.composition;

import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.program.CurriculumDto;
import com.example.interhubdev.program.ProgramDto;
import com.example.interhubdev.user.UserDto;

/**
 * One row for the teacher student groups page: group with program, curriculum, curator, and optional student count.
 */
public record TeacherStudentGroupItemDto(
    StudentGroupDto group,
    ProgramDto program,
    CurriculumDto curriculum,
    UserDto curatorUser,
    Integer studentCount
) {
}
