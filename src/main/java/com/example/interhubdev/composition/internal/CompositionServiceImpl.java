package com.example.interhubdev.composition.internal;

import com.example.interhubdev.composition.CompositionApi;
import com.example.interhubdev.composition.GroupSubjectInfoDto;
import com.example.interhubdev.composition.LessonFullDetailsDto;
import com.example.interhubdev.composition.LessonHomeworkSubmissionsDto;
import com.example.interhubdev.composition.LessonRosterAttendanceDto;
import com.example.interhubdev.composition.StudentAttendanceHistoryDto;
import com.example.interhubdev.composition.StudentGradeHistoryDto;
import com.example.interhubdev.composition.StudentHomeworkHistoryDto;
import com.example.interhubdev.composition.StudentSubjectsDto;
import com.example.interhubdev.composition.TeacherStudentGroupsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Facade implementing CompositionApi: delegates to per-use-case services.
 * Read-only composition; no business logic here.
 */
@Service
@RequiredArgsConstructor
class CompositionServiceImpl implements CompositionApi {

    private final LessonFullDetailsService lessonFullDetailsService;
    private final LessonRosterAttendanceService lessonRosterAttendanceService;
    private final LessonHomeworkSubmissionsService lessonHomeworkSubmissionsService;
    private final TeacherStudentGroupsService teacherStudentGroupsService;
    private final StudentSubjectsService studentSubjectsService;
    private final GroupSubjectInfoService groupSubjectInfoService;
    private final StudentGradeHistoryService studentGradeHistoryService;
    private final StudentAttendanceHistoryService studentAttendanceHistoryService;
    private final StudentHomeworkHistoryService studentHomeworkHistoryService;

    @Override
    public LessonFullDetailsDto getLessonFullDetails(UUID lessonId, UUID requesterId) {
        return lessonFullDetailsService.execute(lessonId, requesterId);
    }

    @Override
    public LessonRosterAttendanceDto getLessonRosterAttendance(UUID lessonId, UUID requesterId, boolean includeCanceled) {
        return lessonRosterAttendanceService.execute(lessonId, requesterId, includeCanceled);
    }

    @Override
    public LessonHomeworkSubmissionsDto getLessonHomeworkSubmissions(UUID lessonId, UUID requesterId) {
        return lessonHomeworkSubmissionsService.execute(lessonId, requesterId);
    }

    @Override
    public TeacherStudentGroupsDto getTeacherStudentGroups(UUID requesterId) {
        return teacherStudentGroupsService.execute(requesterId);
    }

    @Override
    public StudentSubjectsDto getStudentSubjects(UUID requesterId) {
        return studentSubjectsService.execute(requesterId);
    }

    @Override
    public GroupSubjectInfoDto getGroupSubjectInfo(UUID groupId, UUID subjectId, UUID requesterId, Optional<UUID> semesterId) {
        return groupSubjectInfoService.execute(groupId, subjectId, requesterId, semesterId);
    }

    @Override
    public StudentGradeHistoryDto getStudentGradeHistory(UUID studentId, UUID offeringId, UUID requesterId) {
        return studentGradeHistoryService.execute(studentId, offeringId, requesterId);
    }

    @Override
    public StudentAttendanceHistoryDto getStudentAttendanceHistory(UUID studentId, UUID offeringId, UUID requesterId) {
        return studentAttendanceHistoryService.execute(studentId, offeringId, requesterId);
    }

    @Override
    public StudentHomeworkHistoryDto getStudentHomeworkHistory(UUID studentId, UUID offeringId, UUID requesterId) {
        return studentHomeworkHistoryService.execute(studentId, offeringId, requesterId);
    }
}
