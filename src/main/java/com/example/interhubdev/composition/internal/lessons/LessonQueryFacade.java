package com.example.interhubdev.composition.internal.lessons;

import com.example.interhubdev.composition.LessonFullDetailsDto;
import com.example.interhubdev.composition.LessonHomeworkSubmissionsDto;
import com.example.interhubdev.composition.LessonQueryApi;
import com.example.interhubdev.composition.LessonRosterAttendanceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Facade implementing LessonQueryApi; delegates to per-use-case services.
 */
@Service
@RequiredArgsConstructor
class LessonQueryFacade implements LessonQueryApi {

    private final LessonFullDetailsService lessonFullDetailsService;
    private final LessonRosterAttendanceService lessonRosterAttendanceService;
    private final LessonHomeworkSubmissionsService lessonHomeworkSubmissionsService;

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
}
