package com.example.interhubdev.notification.internal.infrastructure.handlers;

import com.example.interhubdev.absencenotice.AbsenceNoticeType;
import com.example.interhubdev.notification.NotificationTemplateKeys;
import com.example.interhubdev.notification.internal.application.CreateNotificationUseCase;
import com.example.interhubdev.notification.internal.domain.Notification;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import com.example.interhubdev.outbox.OutboxEvent;
import com.example.interhubdev.outbox.OutboxEventHandler;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handler for attendance.absence_notice.updated event.
 * <p>
 * Creates notifications for teachers of the lesson session when a student updates an absence notice.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class AbsenceNoticeUpdatedHandler implements OutboxEventHandler {

    private final CreateNotificationUseCase createNotificationUseCase;
    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final TeacherApi teacherApi;
    private final ObjectMapper objectMapper;

    @Override
    public String eventType() {
        return "attendance.absence_notice.updated";
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        Map<String, Object> payload = event.getPayload();

        // Parse payload
        UUID noticeId = UUID.fromString(payload.get("noticeId").toString());
        UUID sessionId = UUID.fromString(payload.get("sessionId").toString());
        UUID studentId = UUID.fromString(payload.get("studentId").toString());
        AbsenceNoticeType noticeType = AbsenceNoticeType.valueOf(payload.get("type").toString());
        Instant occurredAt = parseInstantFromPayload(payload.get("updatedAt"));

        log.debug("Processing absence notice updated event: noticeId={}, sessionId={}, studentId={}",
                noticeId, sessionId, studentId);

        // Get lesson to find offeringId
        LessonDto lesson = scheduleApi.findLessonById(sessionId)
                .orElseThrow(() -> new IllegalStateException("Lesson not found: " + sessionId));

        UUID offeringId = lesson.offeringId();

        // Get teachers for the offering
        List<OfferingTeacherItemDto> offeringTeachers = offeringApi.findTeachersByOfferingId(offeringId);
        if (offeringTeachers.isEmpty()) {
            log.warn("No teachers found for offering: offeringId={}, sessionId={}", offeringId, sessionId);
            return;
        }

        // Get teacher entity IDs and batch-load teacher profiles
        List<UUID> teacherEntityIds = offeringTeachers.stream()
                .map(OfferingTeacherItemDto::teacherId)
                .distinct()
                .collect(Collectors.toList());

        List<TeacherDto> teachers = teacherApi.findByIds(teacherEntityIds);
        if (teachers.isEmpty()) {
            log.warn("No teacher profiles found for offering: offeringId={}", offeringId);
            return;
        }

        // Create notifications for each teacher (user ID)
        for (TeacherDto teacher : teachers) {
            UUID teacherUserId = teacher.userId();

            // Build params JSON
            Map<String, Object> params = Map.of(
                    "sessionId", sessionId.toString(),
                    "noticeId", noticeId.toString(),
                    "studentId", studentId.toString(),
                    "noticeType", noticeType.name()
            );

            // Build data JSON for deep-linking
            Map<String, Object> data = Map.of(
                    "route", "sessionAttendance",
                    "sessionId", sessionId.toString(),
                    "focus", "notices",
                    "noticeId", noticeId.toString(),
                    "studentId", studentId.toString()
            );

            String paramsJson = objectMapper.writeValueAsString(params);
            String dataJson = objectMapper.writeValueAsString(data);

            Notification notification = new Notification(
                    teacherUserId,
                    NotificationTemplateKeys.ABSENCE_NOTICE_UPDATED,
                    paramsJson,
                    dataJson,
                    event.getId(),
                    event.getEventType(),
                    occurredAt
            );

            createNotificationUseCase.execute(notification);
        }

        log.info("Created notifications for {} teachers: noticeId={}, sessionId={}", teachers.size(), noticeId, sessionId);

        // TODO: In future, after creating in-app notification, enqueue push delivery (mobile) based on user preferences.
    }

    private static Instant parseInstantFromPayload(Object value) {
        if (value == null) return Instant.now();
        if (value instanceof Number n) return Instant.ofEpochMilli(n.longValue());
        return Instant.parse(value.toString());
    }
}
