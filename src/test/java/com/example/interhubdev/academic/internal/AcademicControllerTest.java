package com.example.interhubdev.academic.internal;

import com.example.interhubdev.academic.AcademicApi;
import com.example.interhubdev.academic.SemesterDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for Academic: createSemester validation (number 1 or 2).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AcademicController")
class AcademicControllerTest {

    private static final UUID ACADEMIC_YEAR_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcademicApi academicApi;

    @Nested
    @DisplayName("POST /api/academic/years/{academicYearId}/semesters")
    class CreateSemester {

        @Test
        @DisplayName("returns 400 when number is 3")
        void returns400WhenNumberThree() throws Exception {
            String body = """
                    {"number":3,"name":null,"startDate":"2024-09-01","endDate":"2024-12-31","examStartDate":null,"examEndDate":null,"weekCount":16,"isCurrent":false}
                    """;

            mockMvc.perform(post("/api/academic/years/{academicYearId}/semesters", ACADEMIC_YEAR_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verify(academicApi, never()).createSemester(any(), anyInt(), any(), any(), any(), any(), any(), any(), anyBoolean());
        }

        @Test
        @WithMockUser(roles = "MODERATOR")
        @DisplayName("returns 201 when number is 1")
        void returns201WhenNumberOne() throws Exception {
            SemesterDto dto = new SemesterDto(
                    UUID.randomUUID(), ACADEMIC_YEAR_ID, 1, "Fall",
                    LocalDate.of(2024, 9, 1), LocalDate.of(2024, 12, 31),
                    null, null, 16, false, LocalDateTime.now());
            when(academicApi.createSemester(eq(ACADEMIC_YEAR_ID), eq(1), any(), eq(LocalDate.of(2024, 9, 1)), eq(LocalDate.of(2024, 12, 31)),
                    any(), any(), any(), anyBoolean())).thenReturn(dto);

            String body = """
                    {"number":1,"name":"Fall","startDate":"2024-09-01","endDate":"2024-12-31","examStartDate":null,"examEndDate":null,"weekCount":16,"isCurrent":false}
                    """;

            mockMvc.perform(post("/api/academic/years/{academicYearId}/semesters", ACADEMIC_YEAR_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());

            verify(academicApi).createSemester(eq(ACADEMIC_YEAR_ID), eq(1), any(), eq(LocalDate.of(2024, 9, 1)), eq(LocalDate.of(2024, 12, 31)),
                    any(), any(), any(), eq(false));
        }
    }
}
