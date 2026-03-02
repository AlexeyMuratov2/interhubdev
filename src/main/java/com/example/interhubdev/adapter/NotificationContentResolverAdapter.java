package com.example.interhubdev.adapter;

import com.example.interhubdev.notification.NotificationContentResolver;
import com.example.interhubdev.notification.NotificationTemplateKeys;
import com.example.interhubdev.notification.ResolvedNotificationContent;
import com.example.interhubdev.notification.ResolvedNotificationItem;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.UserApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
 * Also resolves homework submission submitted: lesson, subject name, student display name, recipients (teachers).
 * Resolves schedule.lesson.rescheduled and schedule.lesson.deleted: subject name, date/time, recipients (students in group).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationContentResolverAdapter implements NotificationContentResolver {

    private static final String EVENT_ABSENCE_NOTICE_SUBMITTED = "attendance.absence_notice.submitted";
    private static final String EVENT_ABSENCE_NOTICE_UPDATED = "attendance.absence_notice.updated";
    private static final String EVENT_HOMEWORK_SUBMISSION_SUBMITTED = "submission.homework_submission.submitted";
    private static final String EVENT_LESSON_RESCHEDULED = "schedule.lesson.rescheduled";
    private static final String EVENT_LESSON_DELETED = "schedule.lesson.deleted";

    private final StudentApi studentApi;
    private final UserApi userApi;
    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final TeacherApi teacherApi;
    private final ProgramApi programApi;

    @Override
    public Optional<ResolvedNotificationContent> resolve(String eventType, Map<String, Object> payload) {
        if (EVENT_ABSENCE_NOTICE_SUBMITTED.equals(eventType)) {
            return resolveAbsenceNoticeSubmitted(payload);
        }
        if (EVENT_ABSENCE_NOTICE_UPDATED.equals(eventType)) {
            return resolveAbsenceNoticeUpdated(payload);
        }
        if (EVENT_HOMEWORK_SUBMISSION_SUBMITTED.equals(eventType)) {
            return resolveHomeworkSubmissionSubmitted(payload);
        }
        if (EVENT_LESSON_RESCHEDULED.equals(eventType)) {
            return resolveLessonRescheduled(payload);
        }
        if (EVENT_LESSON_DELETED.equals(eventType)) {
            return resolveLessonDeleted(payload);
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
                .map(student -> studentApi.studentDisplayName(student,
                        userApi.findById(student.userId()).map(u -> u.getFullName()).orElse("")))
                .orElse("—");

        UUID singleOfferingId = parseUuid(payload.get("singleOfferingId"));
        String subjectName = null;
        if (singleOfferingId != null) {
            subjectName = offeringApi.findOfferingById(singleOfferingId)
                    .filter(o -> o.curriculumSubjectId() != null)
                    .map(o -> programApi.getSubjectNamesByCurriculumSubjectIds(List.of(o.curriculumSubjectId()))
                            .getOrDefault(o.curriculumSubjectId(), "—"))
                    .orElse("—");
        }

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
            if (subjectName != null) {
                params.put("subjectName", subjectName);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("route", "sessionAttendance");
            data.put("sessionId", firstSessionId.toString());
            data.put("focus", "notices");
            data.put("noticeId", noticeId.toString());
            data.put("studentId", studentId.toString());
            if (subjectName != null) {
                data.put("subjectName", subjectName);
            }

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

    private Optional<ResolvedNotificationContent> resolveHomeworkSubmissionSubmitted(Map<String, Object> payload) {
        UUID submissionId = parseUuid(payload.get("submissionId"));
        UUID homeworkId = parseUuid(payload.get("homeworkId"));
        UUID lessonId = parseUuid(payload.get("lessonId"));
        UUID authorId = parseUuid(payload.get("authorId"));
        Instant submittedAt = parseInstant(payload.get("submittedAt"));
        if (submissionId == null || homeworkId == null || lessonId == null || authorId == null) {
            log.warn("Missing required payload fields for homework submission: submissionId={}, homeworkId={}, lessonId={}, authorId={}",
                    submissionId, homeworkId, lessonId, authorId);
            return Optional.empty();
        }

        Optional<LessonDto> lessonOpt = scheduleApi.findLessonById(lessonId);
        if (lessonOpt.isEmpty()) {
            log.warn("Lesson not found for homework submission event: lessonId={}", lessonId);
            return Optional.empty();
        }
        LessonDto lesson = lessonOpt.get();
        var offeringOpt = offeringApi.findOfferingById(lesson.offeringId());
        if (offeringOpt.isEmpty()) {
            log.warn("Offering not found for homework submission event: offeringId={}", lesson.offeringId());
            return Optional.empty();
        }
        var offering = offeringOpt.get();
        String subjectName = "—";
        if (offering.curriculumSubjectId() != null) {
            Map<UUID, String> subjectNames = programApi.getSubjectNamesByCurriculumSubjectIds(List.of(offering.curriculumSubjectId()));
            subjectName = subjectNames.getOrDefault(offering.curriculumSubjectId(), "—");
        }

        StringBuilder lessonDisplaySb = new StringBuilder(lesson.date().format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (lesson.startTime() != null) {
            lessonDisplaySb.append(" ").append(lesson.startTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
            if (lesson.endTime() != null) {
                lessonDisplaySb.append("–").append(lesson.endTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
            }
        }
        String lessonDisplay = lessonDisplaySb.toString();

        Optional<StudentDto> studentOpt = studentApi.findByUserId(authorId);
        String userFullName = userApi.findById(authorId).map(u -> u.getFullName()).orElse("");
        String studentDisplayName = studentOpt
                .map(s -> studentApi.studentDisplayName(s, userFullName))
                .orElse(userFullName != null && !userFullName.isBlank() ? userFullName : "—");

        Set<UUID> teacherUserIds = new HashSet<>();
        List<OfferingTeacherItemDto> offeringTeachers = offeringApi.findTeachersByOfferingId(lesson.offeringId());
        for (OfferingTeacherItemDto ot : offeringTeachers) {
            teacherApi.findById(ot.teacherId())
                    .map(TeacherDto::userId)
                    .ifPresent(teacherUserIds::add);
        }
        if (teacherUserIds.isEmpty()) {
            log.warn("No teachers found for lesson: lessonId={}", lessonId);
            return Optional.empty();
        }

        List<ResolvedNotificationItem> items = new ArrayList<>();
        for (UUID teacherUserId : teacherUserIds) {
            Map<String, Object> params = new HashMap<>();
            params.put("lessonId", lessonId.toString());
            params.put("lessonDisplay", lessonDisplay);
            params.put("subjectName", subjectName);
            params.put("studentName", studentDisplayName);
            params.put("homeworkId", homeworkId.toString());
            params.put("submissionId", submissionId.toString());
            params.put("studentId", authorId.toString());

            Map<String, Object> data = new HashMap<>();
            data.put("route", "lessonHomeworkSubmissions");
            data.put("lessonId", lessonId.toString());
            data.put("homeworkId", homeworkId.toString());
            data.put("submissionId", submissionId.toString());
            data.put("studentId", authorId.toString());

            items.add(new ResolvedNotificationItem(
                    teacherUserId,
                    NotificationTemplateKeys.HOMEWORK_SUBMISSION_SUBMITTED,
                    params,
                    data
            ));
        }

        Instant occurred = submittedAt != null ? submittedAt : Instant.now();
        return Optional.of(new ResolvedNotificationContent(items, occurred));
    }

    private Optional<ResolvedNotificationContent> resolveLessonRescheduled(Map<String, Object> payload) {
        UUID lessonId = parseUuid(payload.get("lessonId"));
        UUID offeringId = parseUuid(payload.get("offeringId"));
        String dateStr = payload.get("date") != null ? payload.get("date").toString() : null;
        String oldStartTime = payload.get("oldStartTime") != null ? payload.get("oldStartTime").toString() : "";
        String oldEndTime = payload.get("oldEndTime") != null ? payload.get("oldEndTime").toString() : "";
        String newStartTime = payload.get("newStartTime") != null ? payload.get("newStartTime").toString() : "";
        String newEndTime = payload.get("newEndTime") != null ? payload.get("newEndTime").toString() : "";
        if (lessonId == null || offeringId == null || dateStr == null || dateStr.isBlank()) {
            log.warn("Missing required payload for lesson rescheduled: lessonId={}, offeringId={}, date={}",
                    lessonId, offeringId, dateStr);
            return Optional.empty();
        }
        var offeringOpt = offeringApi.findOfferingById(offeringId);
        if (offeringOpt.isEmpty()) {
            log.warn("Offering not found for lesson rescheduled event: offeringId={}", offeringId);
            return Optional.empty();
        }
        var offering = offeringOpt.get();
        String subjectName = "—";
        if (offering.curriculumSubjectId() != null) {
            Map<UUID, String> subjectNames = programApi.getSubjectNamesByCurriculumSubjectIds(List.of(offering.curriculumSubjectId()));
            subjectName = subjectNames.getOrDefault(offering.curriculumSubjectId(), "—");
        }
        String oldDateTime = formatDateTimeRange(dateStr, oldStartTime, oldEndTime);
        String newDateTime = formatDateTimeRange(dateStr, newStartTime, newEndTime);
        List<StudentDto> students = studentApi.findByGroupId(offering.groupId());
        Instant occurred = parseInstant(payload.get("occurredAt"));
        if (occurred == null) occurred = Instant.now();
        if (students.isEmpty()) {
            log.debug("No students in group for lesson rescheduled: offeringId={}, groupId={}", offeringId, offering.groupId());
            return Optional.of(new ResolvedNotificationContent(List.of(), occurred));
        }
        List<ResolvedNotificationItem> items = new ArrayList<>();
        for (StudentDto student : students) {
            Map<String, Object> params = new HashMap<>();
            params.put("lessonId", lessonId.toString());
            params.put("offeringId", offeringId.toString());
            params.put("subjectName", subjectName);
            params.put("oldDateTime", oldDateTime);
            params.put("newDateTime", newDateTime);
            Map<String, Object> data = new HashMap<>();
            data.put("route", "schedule");
            data.put("lessonId", lessonId.toString());
            data.put("offeringId", offeringId.toString());
            items.add(new ResolvedNotificationItem(
                    student.userId(),
                    NotificationTemplateKeys.LESSON_RESCHEDULED,
                    params,
                    data
            ));
        }
        return Optional.of(new ResolvedNotificationContent(items, occurred));
    }

    private Optional<ResolvedNotificationContent> resolveLessonDeleted(Map<String, Object> payload) {
        UUID lessonId = parseUuid(payload.get("lessonId"));
        UUID offeringId = parseUuid(payload.get("offeringId"));
        String dateStr = payload.get("date") != null ? payload.get("date").toString() : null;
        if (lessonId == null || offeringId == null || dateStr == null || dateStr.isBlank()) {
            log.warn("Missing required payload for lesson deleted: lessonId={}, offeringId={}, date={}",
                    lessonId, offeringId, dateStr);
            return Optional.empty();
        }
        var offeringOpt = offeringApi.findOfferingById(offeringId);
        if (offeringOpt.isEmpty()) {
            log.warn("Offering not found for lesson deleted event: offeringId={}", offeringId);
            return Optional.empty();
        }
        var offering = offeringOpt.get();
        String subjectName = "—";
        if (offering.curriculumSubjectId() != null) {
            Map<UUID, String> subjectNames = programApi.getSubjectNamesByCurriculumSubjectIds(List.of(offering.curriculumSubjectId()));
            subjectName = subjectNames.getOrDefault(offering.curriculumSubjectId(), "—");
        }
        List<StudentDto> students = studentApi.findByGroupId(offering.groupId());
        Instant occurred = parseInstant(payload.get("occurredAt"));
        if (occurred == null) occurred = Instant.now();
        if (students.isEmpty()) {
            log.debug("No students in group for lesson deleted: offeringId={}, groupId={}", offeringId, offering.groupId());
            return Optional.of(new ResolvedNotificationContent(List.of(), occurred));
        }
        List<ResolvedNotificationItem> items = new ArrayList<>();
        for (StudentDto student : students) {
            Map<String, Object> params = new HashMap<>();
            params.put("lessonId", lessonId.toString());
            params.put("offeringId", offeringId.toString());
            params.put("subjectName", subjectName);
            params.put("lessonDate", dateStr);
            Map<String, Object> data = new HashMap<>();
            data.put("route", "schedule");
            data.put("lessonId", lessonId.toString());
            data.put("offeringId", offeringId.toString());
            items.add(new ResolvedNotificationItem(
                    student.userId(),
                    NotificationTemplateKeys.LESSON_DELETED,
                    params,
                    data
            ));
        }
        return Optional.of(new ResolvedNotificationContent(items, occurred));
    }

    /** Format "date HH:mm–HH:mm" from date string and time strings (e.g. HH:mm:ss). */
    private static String formatDateTimeRange(String dateStr, String startTime, String endTime) {
        String start = (startTime != null && startTime.length() >= 5) ? startTime.substring(0, 5) : (startTime != null ? startTime : "");
        String end = (endTime != null && endTime.length() >= 5) ? endTime.substring(0, 5) : (endTime != null ? endTime : "");
        if (start.isEmpty() && end.isEmpty()) return dateStr != null ? dateStr : "";
        if (end.isEmpty()) return dateStr + " " + start;
        return dateStr + " " + start + "–" + end;
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
