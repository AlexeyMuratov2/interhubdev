package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gateway for lesson session information.
 */
final class SessionGateway {

    private final ScheduleApi scheduleApi;

    SessionGateway(ScheduleApi scheduleApi) {
        this.scheduleApi = scheduleApi;
    }

    Optional<LessonDto> getSessionById(UUID sessionId) {
        return scheduleApi.findLessonById(sessionId);
    }

    List<LessonDto> getSessionsByIds(List<UUID> sessionIds) {
        return sessionIds.isEmpty() ? List.of() : scheduleApi.findLessonsByIds(sessionIds);
    }
}
