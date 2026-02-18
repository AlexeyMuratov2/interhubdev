package com.example.interhubdev.grades.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.grades.*;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GradesController: happy path create, get by id, get student offering grades.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("GradesController")
class GradesControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID OFFERING_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthApi authApi;

    @MockitoBean
    private GradesApi gradesApi;

    private static UserDto teacher() {
        return new UserDto(
                USER_ID, "t@test.com", List.of(Role.TEACHER), UserStatus.ACTIVE,
                "Teacher", "User", null, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/grades/entries")
    class CreateEntry {

        @Test
        @DisplayName("returns 201 and GradeEntryDto when valid request")
        void success() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(teacher()));
            UUID entryId = UUID.randomUUID();
            GradeEntryDto dto = new GradeEntryDto(
                    entryId, STUDENT_ID, OFFERING_ID, new BigDecimal("10"),
                    GradeTypeCode.SEMINAR, Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), USER_ID, LocalDateTime.now(), "ACTIVE");
            when(gradesApi.create(eq(STUDENT_ID), eq(OFFERING_ID), any(), eq(GradeTypeCode.SEMINAR), isNull(), isNull(),
                    isNull(), isNull(), isNull(), eq(USER_ID))).thenReturn(dto);

            CreateGradeEntryRequest body = new CreateGradeEntryRequest(
                    STUDENT_ID, OFFERING_ID, new BigDecimal("10"),
                    GradeTypeCode.SEMINAR, null, null, null, null, null);

            mockMvc.perform(post("/api/grades/entries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(entryId.toString()))
                    .andExpect(jsonPath("$.studentId").value(STUDENT_ID.toString()))
                    .andExpect(jsonPath("$.offeringId").value(OFFERING_ID.toString()))
                    .andExpect(jsonPath("$.points").value(10))
                    .andExpect(jsonPath("$.typeCode").value("SEMINAR"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(gradesApi).create(eq(STUDENT_ID), eq(OFFERING_ID), eq(new BigDecimal("10")),
                    eq(GradeTypeCode.SEMINAR), isNull(), isNull(), isNull(), isNull(), isNull(), eq(USER_ID));
        }

        @Test
        @DisplayName("returns 401 when not authenticated")
        void unauthorized() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.empty());

            CreateGradeEntryRequest body = new CreateGradeEntryRequest(
                    STUDENT_ID, OFFERING_ID, BigDecimal.TEN, GradeTypeCode.SEMINAR, null, null, null, null, null);

            mockMvc.perform(post("/api/grades/entries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized());
            verify(gradesApi, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/grades/entries/{id}")
    class VoidEntry {

        @Test
        @DisplayName("returns 204 and calls voidEntry")
        void success() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(teacher()));
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/grades/entries/" + id))
                    .andExpect(status().isNoContent());

            verify(gradesApi).voidEntry(eq(id), eq(USER_ID));
        }
    }

    @Nested
    @DisplayName("GET /api/grades/students/{studentId}/offerings/{offeringId}")
    class GetStudentOfferingGrades {

        @Test
        @DisplayName("returns 200 and StudentOfferingGradesDto")
        void success() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(teacher()));
            StudentOfferingGradesDto dto = new StudentOfferingGradesDto(
                    STUDENT_ID, OFFERING_ID,
                    List.of(),
                    BigDecimal.ZERO,
                    java.util.Map.of());
            when(gradesApi.getStudentOfferingGrades(eq(STUDENT_ID), eq(OFFERING_ID), isNull(), isNull(), eq(false), eq(USER_ID)))
                    .thenReturn(dto);

            mockMvc.perform(get("/api/grades/students/" + STUDENT_ID + "/offerings/" + OFFERING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentId").value(STUDENT_ID.toString()))
                    .andExpect(jsonPath("$.offeringId").value(OFFERING_ID.toString()))
                    .andExpect(jsonPath("$.totalPoints").value(0))
                    .andExpect(jsonPath("$.entries").isArray())
                    .andExpect(jsonPath("$.breakdownByType").isMap());

            verify(gradesApi).getStudentOfferingGrades(eq(STUDENT_ID), eq(OFFERING_ID), isNull(), isNull(), eq(false), eq(USER_ID));
        }
    }
}
