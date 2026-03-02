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

/**
 * Handler for attendance.absence_notice.updated event.
 * <p>
 * Creates one notification per teacher (deduped across lessons). Payload has sessionIds (list).
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

        UUID noticeId = UUID.fromString(payload.get("noticeId").toString());
        UUID studentId = UUID.fromString(payload.get("studentId").toString());
        AbsenceNoticeType noticeType = AbsenceNoticeType.valueOf(payload.get("type").toString());
        Instant occurredAt = parseInstantFromPayload(payload.get("updatedAt"));

        @SuppressWarnings("unchecked")
        List<String> sessionIdStrs = (List<String>) payload.get("sessionIds");
        if (sessionIdStrs == null || sessionIdStrs.isEmpty()) {
            log.warn("No sessionIds in payload: noticeId={}", noticeId);
            return;
        }
        List<UUID> sessionIds = sessionIdStrs.stream().map(UUID::fromString).toList();

        log.debug("Processing absence notice updated event: noticeId={}, sessionIds={}, studentId={}",
                noticeId, sessionIds.size(), studentId);

        Set<UUID> teacherUserIds = new HashSet<>();
        UUID firstSessionId = sessionIds.get(0);
        for (UUID sessionId : sessionIds) {
            LessonDto lesson = scheduleApi.findLessonById(sessionId).orElse(null);
            if (lesson == null) continue;
            List<OfferingTeacherItemDto> offeringTeachers = offeringApi.findTeachersByOfferingId(lesson.offeringId());
            for (OfferingTeacherItemDto ot : offeringTeachers) {
                teacherApi.findById(ot.teacherId()).map(TeacherDto::userId).ifPresent(teacherUserIds::add);
            }
        }

        if (teacherUserIds.isEmpty()) {
            log.warn("No teachers found for notice sessions: noticeId={}", noticeId);
            return;
        }

        for (UUID teacherUserId : teacherUserIds) {
            Map<String, Object> params = Map.of(
                    "sessionIds", sessionIds.stream().map(UUID::toString).toList(),
                    "noticeId", noticeId.toString(),
                    "studentId", studentId.toString(),
                    "noticeType", noticeType.name()
            );
            Map<String, Object> data = Map.of(
                    "route", "sessionAttendance",
                    "sessionId", firstSessionId.toString(),
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

        log.info("Created notifications for {} teachers: noticeId={}", teacherUserIds.size(), noticeId);
    }

    private static Instant parseInstantFromPayload(Object value) {
        if (value == null) return Instant.now();
        if (value instanceof Number n) return Instant.ofEpochMilli(n.longValue());
        return Instant.parse(value.toString());
    }
}
