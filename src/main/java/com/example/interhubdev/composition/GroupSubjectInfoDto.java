package com.example.interhubdev.composition;

import com.example.interhubdev.academic.SemesterDto;
import com.example.interhubdev.group.GroupLeaderDetailDto;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import com.example.interhubdev.program.CurriculumDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.ProgramDto;
import com.example.interhubdev.subject.SubjectDto;

import java.util.List;

/**
 * Aggregated data for the teacher's "Group subject info" screen: subject, group, offering, slots,
 * curriculum, semester, total homework count, and per-student stats (points, submissions, attendance).
 */
public record GroupSubjectInfoDto(
    SubjectDto subject,
    StudentGroupDto group,
    /** Group leaders (headman, deputy) with student and user data. */
    List<GroupLeaderDetailDto> leaders,
    ProgramDto program,
    GroupSubjectOfferingDto offering,
    List<OfferingSlotDto> slots,
    List<OfferingTeacherItemDto> teachers,
    CurriculumSubjectDto curriculumSubject,
    CurriculumDto curriculum,
    /** All curriculum subjects in the group's curriculum (study plan). */
    List<CurriculumSubjectDto> curriculumSubjects,
    SemesterDto semester,
    /** Total homework assignments for this offering in the semester. */
    int totalHomeworkCount,
    /** Students with points, submitted count, and attendance percent. */
    List<GroupSubjectStudentItemDto> students
) {
}
