package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.*;
import com.example.interhubdev.attendance.internal.integration.AbsenceNoticeAttachedEventPayload;
import com.example.interhubdev.attendance.internal.integration.AbsenceNoticeCanceledEventPayload;
import com.example.interhubdev.attendance.internal.integration.AbsenceNoticeSubmittedEventPayload;
import com.example.interhubdev.attendance.internal.integration.AbsenceNoticeUpdatedEventPayload;
import com.example.interhubdev.attendance.internal.integration.AttendanceMarkedEventPayload;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingTeacherDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Attendance module outbox events.
 * Verifies that integration events are published correctly when attendance-related operations occur.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Attendance Outbox Events Integration")
class AttendanceOutboxEventsIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubmitOrUpdateAbsenceNoticeUseCase submitOrUpdateAbsenceNoticeUseCase;

    @Autowired
    private CancelAbsenceNoticeUseCase cancelAbsenceNoticeUseCase;

    @Autowired
    private AttachAbsenceNoticeToAttendanceUseCase attachAbsenceNoticeToAttendanceUseCase;

    @Autowired
    private AttendanceServiceImpl attendanceService;

    @MockitoBean
    private ScheduleApi scheduleApi;

    @MockitoBean
    private OfferingApi offeringApi;

    @MockitoBean
    private StudentApi studentApi;

    @MockitoBean
    private GroupApi groupApi;

    @MockitoBean
    private TeacherApi teacherApi;

    @MockitoBean
    private UserApi userApi;

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID OFFERING_ID = UUID.randomUUID();
    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Clean up outbox events before each test
        jdbcTemplate.update("DELETE FROM outbox_event");

        // Setup common mocks
        LessonDto lesson = new LessonDto(
                SESSION_ID, OFFERING_ID, UUID.randomUUID(), LocalDate.now(),
                null, null, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(scheduleApi.findLessonById(SESSION_ID)).thenReturn(Optional.of(lesson));

        GroupSubjectOfferingDto offering = new GroupSubjectOfferingDto(
                OFFERING_ID, GROUP_ID, UUID.randomUUID(), TEACHER_ID, null,
                "offline", null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(offeringApi.findOfferingById(OFFERING_ID)).thenReturn(Optional.of(offering));
        when(offeringApi.findTeachersByOfferingId(OFFERING_ID)).thenReturn(List.of(
                new OfferingTeacherDto(UUID.randomUUID(), OFFERING_ID, TEACHER_ID, "MAIN", LocalDateTime.now())
        ));

        StudentDto student = new StudentDto(
                STUDENT_ID, USER_ID, "ST001", "Test", "Faculty", "Course",
                2024, "Group1", LocalDateTime.now(), LocalDateTime.now()
        );
        when(studentApi.findByGroupId(GROUP_ID)).thenReturn(List.of(student));
        when(studentApi.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        UserDto teacher = new UserDto(
                USER_ID, "teacher@test.com", List.of(Role.TEACHER), UserStatus.ACTIVE,
                "Teacher", "User", null, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
        when(userApi.findById(any(UUID.class))).thenReturn(Optional.of(teacher));

        TeacherDto teacherProfile = new TeacherDto(
                TEACHER_ID, USER_ID, "T001", "Faculty", "Test Teacher", "Position",
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(teacherApi.findByUserId(USER_ID)).thenReturn(Optional.of(teacherProfile));
    }

    @Nested
    @DisplayName("Absence Notice Events")
    class AbsenceNoticeEvents {

        @Test
        @DisplayName("publishes ABSENCE_NOTICE_SUBMITTED when submitting new notice")
        void publishesSubmittedEvent() {
            // Given
            SubmitAbsenceNoticeRequest request = new SubmitAbsenceNoticeRequest(
                    SESSION_ID, AbsenceNoticeType.ABSENT, "Test reason", List.of()
            );

            // When
            submitOrUpdateAbsenceNoticeUseCase.execute(request, STUDENT_ID);

            // Then
            List<OutboxEvent> events = findEventsByType(AttendanceEventTypes.ABSENCE_NOTICE_SUBMITTED);
            assertThat(events).hasSize(1);

            OutboxEvent event = events.get(0);
            assertThat(event.eventType()).isEqualTo(AttendanceEventTypes.ABSENCE_NOTICE_SUBMITTED);
            assertThat(event.status()).isEqualTo("NEW");
            assertThat(event.occurredAt()).isNotNull();

            // Verify payload
            JsonNode payload = parsePayload(event.payloadJson());
            assertThat(payload.get("noticeId")).isNotNull();
            assertThat(payload.get("sessionId").asText()).isEqualTo(SESSION_ID.toString());
            assertThat(payload.get("studentId").asText()).isEqualTo(STUDENT_ID.toString());
            assertThat(payload.get("type").asText()).isEqualTo("ABSENT");
            assertThat(payload.get("submittedAt")).isNotNull();
        }

        @Test
        @DisplayName("publishes ABSENCE_NOTICE_UPDATED when updating existing notice")
        void publishesUpdatedEvent() {
            // Given - first submit a notice
            SubmitAbsenceNoticeRequest firstRequest = new SubmitAbsenceNoticeRequest(
                    SESSION_ID, AbsenceNoticeType.ABSENT, "First reason", List.of()
            );
            AbsenceNoticeDto firstNotice = submitOrUpdateAbsenceNoticeUseCase.execute(firstRequest, STUDENT_ID);

            // Clear events from first submission
            jdbcTemplate.update("DELETE FROM outbox_event");

            // When - update the notice
            SubmitAbsenceNoticeRequest updateRequest = new SubmitAbsenceNoticeRequest(
                    SESSION_ID, AbsenceNoticeType.LATE, "Updated reason", List.of()
            );
            submitOrUpdateAbsenceNoticeUseCase.execute(updateRequest, STUDENT_ID);

            // Then
            List<OutboxEvent> events = findEventsByType(AttendanceEventTypes.ABSENCE_NOTICE_UPDATED);
            assertThat(events).hasSize(1);

            OutboxEvent event = events.get(0);
            assertThat(event.eventType()).isEqualTo(AttendanceEventTypes.ABSENCE_NOTICE_UPDATED);
            assertThat(event.status()).isEqualTo("NEW");

            // Verify payload
            JsonNode payload = parsePayload(event.payloadJson());
            assertThat(payload.get("noticeId").asText()).isEqualTo(firstNotice.id().toString());
            assertThat(payload.get("sessionId").asText()).isEqualTo(SESSION_ID.toString());
            assertThat(payload.get("studentId").asText()).isEqualTo(STUDENT_ID.toString());
            assertThat(payload.get("type").asText()).isEqualTo("LATE");
            assertThat(payload.get("updatedAt")).isNotNull();
        }

        @Test
        @DisplayName("publishes ABSENCE_NOTICE_CANCELED when canceling notice")
        void publishesCanceledEvent() {
            // Given - submit a notice first
            SubmitAbsenceNoticeRequest request = new SubmitAbsenceNoticeRequest(
                    SESSION_ID, AbsenceNoticeType.ABSENT, "Test reason", List.of()
            );
            AbsenceNoticeDto notice = submitOrUpdateAbsenceNoticeUseCase.execute(request, STUDENT_ID);

            // Clear events from submission
            jdbcTemplate.update("DELETE FROM outbox_event");

            // When
            cancelAbsenceNoticeUseCase.execute(notice.id(), STUDENT_ID);

            // Then
            List<OutboxEvent> events = findEventsByType(AttendanceEventTypes.ABSENCE_NOTICE_CANCELED);
            assertThat(events).hasSize(1);

            OutboxEvent event = events.get(0);
            assertThat(event.eventType()).isEqualTo(AttendanceEventTypes.ABSENCE_NOTICE_CANCELED);
            assertThat(event.status()).isEqualTo("NEW");

            // Verify payload
            JsonNode payload = parsePayload(event.payloadJson());
            assertThat(payload.get("noticeId").asText()).isEqualTo(notice.id().toString());
            assertThat(payload.get("sessionId").asText()).isEqualTo(SESSION_ID.toString());
            assertThat(payload.get("studentId").asText()).isEqualTo(STUDENT_ID.toString());
            assertThat(payload.get("canceledAt")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Attendance Marked Events")
    class AttendanceMarkedEvents {

        @Test
        @DisplayName("publishes ATTENDANCE_MARKED when marking attendance single")
        void publishesMarkedEventForSingle() {
            // When
            attendanceService.markAttendanceSingle(
                    SESSION_ID, STUDENT_ID, AttendanceStatus.PRESENT,
                    null, null, null, null, USER_ID
            );

            // Then
            List<OutboxEvent> events = findEventsByType(AttendanceEventTypes.ATTENDANCE_MARKED);
            assertThat(events).hasSize(1);

            OutboxEvent event = events.get(0);
            assertThat(event.eventType()).isEqualTo(AttendanceEventTypes.ATTENDANCE_MARKED);
            assertThat(event.status()).isEqualTo("NEW");

            // Verify payload
            JsonNode payload = parsePayload(event.payloadJson());
            assertThat(payload.get("recordId")).isNotNull();
            assertThat(payload.get("sessionId").asText()).isEqualTo(SESSION_ID.toString());
            assertThat(payload.get("studentId").asText()).isEqualTo(STUDENT_ID.toString());
            assertThat(payload.get("status").asText()).isEqualTo("PRESENT");
            assertThat(payload.get("markedBy").asText()).isEqualTo(USER_ID.toString());
            assertThat(payload.get("markedAt")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Absence Notice Attached Events")
    class AbsenceNoticeAttachedEvents {

        @Test
        @DisplayName("publishes ABSENCE_NOTICE_ATTACHED when attaching notice to attendance record")
        void publishesAttachedEvent() {
            // Given - create a notice and an attendance record
            SubmitAbsenceNoticeRequest noticeRequest = new SubmitAbsenceNoticeRequest(
                    SESSION_ID, AbsenceNoticeType.ABSENT, "Test reason", List.of()
            );
            AbsenceNoticeDto notice = submitOrUpdateAbsenceNoticeUseCase.execute(noticeRequest, STUDENT_ID);

            AttendanceRecordDto record = attendanceService.markAttendanceSingle(
                    SESSION_ID, STUDENT_ID, AttendanceStatus.EXCUSED,
                    null, null, null, null, USER_ID
            );

            // Clear events from previous operations
            jdbcTemplate.update("DELETE FROM outbox_event");

            // When - attach notice to record
            attachAbsenceNoticeToAttendanceUseCase.execute(record.id(), notice.id(), USER_ID);

            // Then
            List<OutboxEvent> events = findEventsByType(AttendanceEventTypes.ABSENCE_NOTICE_ATTACHED);
            assertThat(events).hasSize(1);

            OutboxEvent event = events.get(0);
            assertThat(event.eventType()).isEqualTo(AttendanceEventTypes.ABSENCE_NOTICE_ATTACHED);
            assertThat(event.status()).isEqualTo("NEW");

            // Verify payload
            JsonNode payload = parsePayload(event.payloadJson());
            assertThat(payload.get("recordId").asText()).isEqualTo(record.id().toString());
            assertThat(payload.get("noticeId").asText()).isEqualTo(notice.id().toString());
            assertThat(payload.get("sessionId").asText()).isEqualTo(SESSION_ID.toString());
            assertThat(payload.get("studentId").asText()).isEqualTo(STUDENT_ID.toString());
            assertThat(payload.get("attachedBy").asText()).isEqualTo(USER_ID.toString());
            assertThat(payload.get("attachedAt")).isNotNull();
        }
    }

    private List<OutboxEvent> findEventsByType(String eventType) {
        return jdbcTemplate.query(
                "SELECT id, event_type, payload_json, occurred_at, status FROM outbox_event WHERE event_type = ?",
                this::mapToOutboxEvent,
                eventType
        );
    }

    private OutboxEvent mapToOutboxEvent(ResultSet rs, int rowNum) throws SQLException {
        return new OutboxEvent(
                rs.getObject("id", UUID.class),
                rs.getString("event_type"),
                rs.getString("payload_json"),
                rs.getObject("occurred_at", Instant.class),
                rs.getString("status")
        );
    }

    private JsonNode parsePayload(String payloadJson) {
        try {
            return objectMapper.readTree(payloadJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse payload JSON", e);
        }
    }

    private record OutboxEvent(
            UUID id,
            String eventType,
            String payloadJson,
            Instant occurredAt,
            String status
    ) {
    }
}
