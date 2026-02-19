package com.example.interhubdev.notification.internal.infrastructure.handlers;

import com.example.interhubdev.attendance.AttendanceStatus;
import com.example.interhubdev.notification.NotificationTemplateKeys;
import com.example.interhubdev.notification.internal.application.CreateNotificationUseCase;
import com.example.interhubdev.notification.internal.domain.Notification;
import com.example.interhubdev.outbox.OutboxEvent;
import com.example.interhubdev.outbox.OutboxEventHandler;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for attendance.record.marked event.
 * <p>
 * Creates notification for the student when attendance is marked.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class AttendanceMarkedHandler implements OutboxEventHandler {

    private final CreateNotificationUseCase createNotificationUseCase;
    private final StudentApi studentApi;
    private final ObjectMapper objectMapper;

    @Override
    public String eventType() {
        return "attendance.record.marked";
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        Map<String, Object> payload = event.getPayload();

        // Parse payload
        UUID recordId = UUID.fromString(payload.get("recordId").toString());
        UUID sessionId = UUID.fromString(payload.get("sessionId").toString());
        UUID studentId = UUID.fromString(payload.get("studentId").toString());
        AttendanceStatus status = AttendanceStatus.valueOf(payload.get("status").toString());
        Instant occurredAt = Instant.parse(payload.get("markedAt").toString());

        log.debug("Processing attendance marked event: recordId={}, sessionId={}, studentId={}, status={}",
                recordId, sessionId, studentId, status);

        // Get student profile to find userId
        StudentDto student = studentApi.findById(studentId)
                .orElseThrow(() -> new IllegalStateException("Student not found: " + studentId));

        UUID studentUserId = student.userId();

        // Build params JSON
        Map<String, Object> params = Map.of(
                "sessionId", sessionId.toString(),
                "recordId", recordId.toString(),
                "status", status.name()
        );

        // Build data JSON for deep-linking
        Map<String, Object> data = Map.of(
                "route", "studentAttendance",
                "sessionId", sessionId.toString(),
                "recordId", recordId.toString()
        );

        String paramsJson = objectMapper.writeValueAsString(params);
        String dataJson = objectMapper.writeValueAsString(data);

        Notification notification = new Notification(
                studentUserId,
                NotificationTemplateKeys.ATTENDANCE_MARKED,
                paramsJson,
                dataJson,
                event.getId(),
                event.getEventType(),
                occurredAt
        );

        createNotificationUseCase.execute(notification);

        log.info("Created notification for student: recordId={}, studentUserId={}", recordId, studentUserId);

        // TODO: In future, after creating in-app notification, enqueue push delivery (mobile) based on user preferences.
    }
}
