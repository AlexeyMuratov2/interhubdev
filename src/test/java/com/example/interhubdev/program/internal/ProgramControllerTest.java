package com.example.interhubdev.program.internal;

import com.example.interhubdev.error.AppException;
import org.springframework.http.HttpStatus;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.ProgramApi;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for Program: createCurriculumSubject validation (semesterNo 1 or 2).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ProgramController")
class ProgramControllerTest {

    private static final UUID CURRICULUM_ID = UUID.randomUUID();
    private static final UUID SUBJECT_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_TYPE_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProgramApi programApi;

    @Nested
    @DisplayName("POST /api/programs/curricula/{curriculumId}/subjects")
    class CreateCurriculumSubject {

        @Test
        @DisplayName("returns 400 when semesterNo is 3")
        void returns400WhenSemesterNoThree() throws Exception {
            String body = """
                    {"subjectId":"%s","semesterNo":3,"courseYear":1,"durationWeeks":16,"hoursTotal":null,"hoursLecture":null,"hoursPractice":null,"hoursLab":null,"hoursSeminar":null,"hoursSelfStudy":null,"hoursConsultation":null,"hoursCourseWork":null,"assessmentTypeId":"%s","credits":1}
                    """
                    .formatted(SUBJECT_ID, ASSESSMENT_TYPE_ID);

            mockMvc.perform(post("/api/programs/curricula/{curriculumId}/subjects", CURRICULUM_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verify(programApi, never()).createCurriculumSubject(any(), any(), anyInt(), any(), anyInt(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @WithMockUser(roles = "MODERATOR")
        @DisplayName("returns 201 when semesterNo is 1 and request valid")
        void returns201WhenSemesterNoOne() throws Exception {
            UUID createdId = UUID.randomUUID();
            CurriculumSubjectDto dto = new CurriculumSubjectDto(
                    createdId, CURRICULUM_ID, SUBJECT_ID, 1, Integer.valueOf(1), 16,
                    null, null, null, null, null, null, null, null,
                    ASSESSMENT_TYPE_ID, BigDecimal.ONE, LocalDateTime.now(), LocalDateTime.now());
            when(programApi.createCurriculumSubject(eq(CURRICULUM_ID), eq(SUBJECT_ID), eq(1), any(), eq(16),
                    any(), any(), any(), any(), any(), any(), any(), any(),
                    eq(ASSESSMENT_TYPE_ID), any())).thenReturn(dto);

            String body = """
                    {"subjectId":"%s","semesterNo":1,"courseYear":1,"durationWeeks":16,"hoursTotal":null,"hoursLecture":null,"hoursPractice":null,"hoursLab":null,"hoursSeminar":null,"hoursSelfStudy":null,"hoursConsultation":null,"hoursCourseWork":null,"assessmentTypeId":"%s","credits":1}
                    """
                    .formatted(SUBJECT_ID, ASSESSMENT_TYPE_ID);

            mockMvc.perform(post("/api/programs/curricula/{curriculumId}/subjects", CURRICULUM_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());

            verify(programApi).createCurriculumSubject(eq(CURRICULUM_ID), eq(SUBJECT_ID), eq(1), any(), eq(16),
                    any(), any(), any(), any(), any(), any(), any(), any(),
                    eq(ASSESSMENT_TYPE_ID), any());
        }
    }

}
