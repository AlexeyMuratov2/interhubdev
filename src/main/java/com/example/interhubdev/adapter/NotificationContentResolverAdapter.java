package com.example.interhubdev.adapter;

import com.example.interhubdev.notification.NotificationContentResolver;
import com.example.interhubdev.notification.NotificationTemplateKeys;
import com.example.interhubdev.notification.ResolvedNotificationContent;
import com.example.interhubdev.notification.ResolvedNotificationItem;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter: implements Notification module's NotificationContentResolver using Student, Schedule,
 * Offering and Teacher modules. Resolves rich notification content for absence notice events
 * (submitted/updated): student display name, period, recipients (teachers), params and data for templates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationContentResolverAdapter implements NotificationContentResolver {

    private static final String EVENT_ABSENCE_NOTICE_SUBMITTED = "attendance.absence_notice.submitted";
    private static final String EVENT_ABSENCE_NOTICE_UPDATED = "attendance.absence_notice.updated";

    private final StudentApi studentApi;
    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final TeacherApi teacherApi;

    @Override
    public Optional<ResolvedNotificationContent> resolve(String eventType, Map<String, Object> payload) {
        if (EVENT_ABSENCE_NOTICE_SUBMITTED.equals(eventType)) {
            return resolveAbsenceNoticeSubmitted(payload);
        }
        if (EVENT_ABSENCE_NOTICE_UPDATED.equals(eventType)) {
            return resolveAbsenceNoticeUpdated(payload);
        }
        return Optional.empty();
    }

    private Optional<ResolvedNotificationContent> resolveAbsenceNoticeSubmitted(Map<String, Object> payload) {
        return resolveAbsenceNotice(
                payload,
                "submittedAt",
                NotificationTemplateKeys.ABSENCE_NOTICE_SUBMITTED
        );
    }

    private Optional<ResolvedNotificationContent> resolveAbsenceNoticeUpdated(Map<String, Object> payload) {
        return resolveAbsenceNotice(
                payload,
                "updatedAt",
                NotificationTemplateKeys.ABSENCE_NOTICE_UPDATED
        );
    }

    private Optional<ResolvedNotificationContent> resolveAbsenceNotice(
            Map<String, Object> payload,
            String timestampKey,
            String templateKey
    ) {
        UUID noticeId = parseUuid(payload.get("noticeId"));
        UUID studentId = parseUuid(payload.get("studentId"));
        String noticeTypeStr = payload.get("type") != null ? payload.get("type").toString() : null;
        Instant sourceOccurredAt = parseInstant(payload.get(timestampKey));
        Instant periodStart = parseInstant(payload.get("periodStart"));
        Instant periodEnd = parseInstant(payload.get("periodEnd"));

        @SuppressWarnings("unchecked")
        List<String> sessionIdStrs = (List<String>) payload.get("sessionIds");
        if (sessionIdStrs == null || sessionIdStrs.isEmpty()) {
            log.warn("No sessionIds in payload: noticeId={}", noticeId);
            return Optional.empty();
        }
        List<UUID> sessionIds = sessionIdStrs.stream()
                .map(NotificationContentResolverAdapter::parseUuid)
                .filter(id -> id != null)
                .toList();
        if (sessionIds.isEmpty()) {
            return Optional.empty();
        }

        String studentDisplayName = studentApi.findById(studentId)
                .map(this::formatStudentDisplayName)
                .orElse("Student");

        Set<UUID> teacherUserIds = new HashSet<>();
        UUID firstSessionId = sessionIds.get(0);
        for (UUID sessionId : sessionIds) {
            Optional<LessonDto> lessonOpt = scheduleApi.findLessonById(sessionId);
            if (lessonOpt.isEmpty()) {
                continue;
            }
            List<OfferingTeacherItemDto> offeringTeachers = offeringApi.findTeachersByOfferingId(lessonOpt.get().offeringId());
            for (OfferingTeacherItemDto ot : offeringTeachers) {
                teacherApi.findById(ot.teacherId())
                        .map(TeacherDto::userId)
                        .ifPresent(teacherUserIds::add);
            }
        }

        if (teacherUserIds.isEmpty()) {
            log.warn("No teachers found for notice sessions: noticeId={}", noticeId);
            return Optional.empty();
        }

        List<ResolvedNotificationItem> items = new ArrayList<>();
        for (UUID teacherUserId : teacherUserIds) {
            Map<String, Object> params = new HashMap<>();
            params.put("sessionIds", sessionIds.stream().map(UUID::toString).toList());
            params.put("noticeId", noticeId.toString());
            params.put("studentId", studentId.toString());
            params.put("noticeType", noticeTypeStr != null ? noticeTypeStr : "");
            params.put("studentName", studentDisplayName);
            params.put("periodStart", periodStart != null ? periodStart.toString() : "");
            params.put("periodEnd", periodEnd != null ? periodEnd.toString() : "");

            Map<String, Object> data = new HashMap<>();
            data.put("route", "sessionAttendance");
            data.put("sessionId", firstSessionId.toString());
            data.put("focus", "notices");
            data.put("noticeId", noticeId.toString());
            data.put("studentId", studentId.toString());

            items.add(new ResolvedNotificationItem(
                    teacherUserId,
                    templateKey,
                    params,
                    data
            ));
        }

        Instant occurred = sourceOccurredAt != null ? sourceOccurredAt : Instant.now();
        return Optional.of(new ResolvedNotificationContent(items, occurred));
    }

    private String formatStudentDisplayName(StudentDto dto) {
        if (dto.chineseName() != null && !dto.chineseName().isBlank()) {
            return dto.chineseName();
        }
        return dto.studentId() != null && !dto.studentId().isBlank() ? dto.studentId() : "Student";
    }

    private static UUID parseUuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID u) return u;
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant parseInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant i) return i;
        if (value instanceof Number n) return Instant.ofEpochMilli(n.longValue());
        try {
            return Instant.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
