package com.example.interhubdev.composition.internal;

import com.example.interhubdev.composition.CompositionApi;
import com.example.interhubdev.composition.LessonFullDetailsDto;
import com.example.interhubdev.composition.LessonHomeworkSubmissionsDto;
import com.example.interhubdev.composition.LessonRosterAttendanceDto;
import com.example.interhubdev.composition.TeacherStudentGroupsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
