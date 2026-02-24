package com.example.interhubdev.composition;

import com.example.interhubdev.academic.SemesterDto;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.program.CurriculumDto;
import com.example.interhubdev.program.ProgramDto;
import com.example.interhubdev.user.UserDto;

import java.util.List;
import java.util.UUID;

/**
 * One row for the teacher student groups page: group with program, curriculum, curator, optional student count,
 * semesters where the teacher has lessons with this group, and subject IDs (for filtering by year, semester, subject).
 */
public record TeacherStudentGroupItemDto(
    StudentGroupDto group,
    ProgramDto program,
    CurriculumDto curriculum,
    UserDto curatorUser,
    Integer studentCount,
    List<SemesterDto> semesters,
    List<UUID> subjectIds
) {
}
