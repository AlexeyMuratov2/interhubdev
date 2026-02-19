package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;

import java.util.Optional;
import java.util.UUID;

/**
 * Gateway for accessing lesson session information.
 * Abstracts ScheduleApi calls for attendance module.
 */
final class SessionGateway {

    private final ScheduleApi scheduleApi;

    SessionGateway(ScheduleApi scheduleApi) {
        this.scheduleApi = scheduleApi;
    }

    /**
     * Get lesson session by ID.
     *
     * @param sessionId lesson session ID
     * @return optional lesson DTO
     */
    Optional<LessonDto> getSessionById(UUID sessionId) {
        return scheduleApi.findLessonById(sessionId);
    }
}
