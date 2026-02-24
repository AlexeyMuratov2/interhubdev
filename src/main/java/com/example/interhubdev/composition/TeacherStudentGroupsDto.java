package com.example.interhubdev.composition;

import com.example.interhubdev.academic.AcademicYearDto;
import com.example.interhubdev.academic.SemesterDto;
import com.example.interhubdev.subject.SubjectDto;

import java.util.List;

/**
 * Response for GET /api/composition/teacher/student-groups.
 * Lists all groups where the current teacher has at least one lesson (slots with lessons only).
 * Includes filter options: academicYears, semesters, subjects; each group item includes semesters and subjectIds for filtering.
 */
public record TeacherStudentGroupsDto(
    List<AcademicYearDto> academicYears,
    List<SemesterDto> semesters,
    List<SubjectDto> subjects,
    List<TeacherStudentGroupItemDto> groups
) {
}
