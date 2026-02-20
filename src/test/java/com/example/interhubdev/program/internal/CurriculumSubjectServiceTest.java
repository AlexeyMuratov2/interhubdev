package com.example.interhubdev.program.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.CurriculumStatus;
import com.example.interhubdev.subject.AssessmentTypeDto;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.subject.SubjectDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CurriculumSubjectService: semester 1–2 validation, course year validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CurriculumSubjectService")
class CurriculumSubjectServiceTest {

    private static final UUID CURRICULUM_ID = UUID.randomUUID();
    private static final UUID SUBJECT_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_TYPE_ID = UUID.randomUUID();

    @Mock
    private CurriculumRepository curriculumRepository;
    @Mock
    private CurriculumSubjectRepository curriculumSubjectRepository;
    @Mock
    private SubjectApi subjectApi;

    @InjectMocks
    private CurriculumSubjectService curriculumSubjectService;

    private static Curriculum curriculum(int durationYears) {
        return Curriculum.builder()
                .id(CURRICULUM_ID)
                .programId(UUID.randomUUID())
                .version("v1")
                .durationYears(durationYears)
                .isActive(true)
                .status(CurriculumStatus.DRAFT)
                .build();
    }

    private static SubjectDto subjectDto() {
        return new SubjectDto(
                SUBJECT_ID, "CODE", null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private static AssessmentTypeDto assessmentTypeDto() {
        return new AssessmentTypeDto(
                ASSESSMENT_TYPE_ID, "EXAM", null, null, true, true, 1, LocalDateTime.now());
    }

    private void givenCurriculumWithDuration(int durationYears) {
        when(curriculumRepository.findById(CURRICULUM_ID))
                .thenReturn(Optional.of(curriculum(durationYears)));
    }

    private void givenSubjectAndAssessmentTypeExist() {
        when(subjectApi.findSubjectById(SUBJECT_ID)).thenReturn(Optional.of(subjectDto()));
        when(subjectApi.findAssessmentTypeById(ASSESSMENT_TYPE_ID)).thenReturn(Optional.of(assessmentTypeDto()));
    }

    private void givenNoDuplicateCurriculumSubject() {
        when(curriculumSubjectRepository.existsByCurriculumIdAndSubjectIdAndSemesterNo(
                eq(CURRICULUM_ID), eq(SUBJECT_ID), any(Integer.class))).thenReturn(false);
    }

    private void givenSaveReturnsEntityWithId() {
        when(curriculumSubjectRepository.save(any(CurriculumSubject.class)))
                .thenAnswer(inv -> {
                    CurriculumSubject e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });
    }

    @Nested
    @DisplayName("createCurriculumSubject")
    class Create {

        @Test
        @DisplayName("throws when semesterNo is 3")
        void throwsWhenSemesterNoThree() {
            givenCurriculumWithDuration(4);
            givenSubjectAndAssessmentTypeExist();

            assertThatThrownBy(() -> curriculumSubjectService.createCurriculumSubject(
                    CURRICULUM_ID, SUBJECT_ID, 3, 1, 16,
                    null, null, null, null, null, null, null, null,
                    ASSESSMENT_TYPE_ID, BigDecimal.ONE))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("semesterNo must be 1 or 2");

            verify(curriculumSubjectRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when courseYear exceeds durationYears")
        void throwsWhenCourseYearExceedsDuration() {
            givenCurriculumWithDuration(4); // max course = 4
            givenSubjectAndAssessmentTypeExist();

            assertThatThrownBy(() -> curriculumSubjectService.createCurriculumSubject(
                    CURRICULUM_ID, SUBJECT_ID, 1, 5, 16,
                    null, null, null, null, null, null, null, null,
                    ASSESSMENT_TYPE_ID, BigDecimal.ONE))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("courseYear must not exceed curriculum duration")
                    .hasMessageContaining("max is 4");

            verify(curriculumSubjectRepository, never()).save(any());
        }

        @Test
        @DisplayName("succeeds when semesterNo is 1 and courseYear in range")
        void succeedsWithValidSemesterNoAndCourseYear() {
            givenCurriculumWithDuration(4);
            givenSubjectAndAssessmentTypeExist();
            givenNoDuplicateCurriculumSubject();
            givenSaveReturnsEntityWithId();

            CurriculumSubjectDto dto = curriculumSubjectService.createCurriculumSubject(
                    CURRICULUM_ID, SUBJECT_ID, 1, 2, 16,
                    null, null, null, null, null, null, null, null,
                    ASSESSMENT_TYPE_ID, BigDecimal.ONE);

            assertThat(dto).isNotNull();
            assertThat(dto.semesterNo()).isEqualTo(1);
            assertThat(dto.courseYear()).isEqualTo(2);
            assertThat(dto.curriculumId()).isEqualTo(CURRICULUM_ID);
            assertThat(dto.subjectId()).isEqualTo(SUBJECT_ID);
            verify(curriculumSubjectRepository).save(any(CurriculumSubject.class));
        }

        @Test
        @DisplayName("throws when courseYear exceeds durationYears")
        void throwsWhenCourseYearExceedsDurationYears() {
            givenCurriculumWithDuration(4); // max course = 4
            givenSubjectAndAssessmentTypeExist();

            assertThatThrownBy(() -> curriculumSubjectService.createCurriculumSubject(
                    CURRICULUM_ID, SUBJECT_ID, 2, 10, 16,
                    null, null, null, null, null, null, null, null,
                    ASSESSMENT_TYPE_ID, BigDecimal.ONE))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("courseYear must not exceed curriculum duration")
                    .hasMessageContaining("max is 4");

            verify(curriculumSubjectRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateCurriculumSubject")
    class Update {

        @Test
        @DisplayName("throws when courseYear exceeds curriculum duration")
        void throwsWhenCourseYearExceedsDuration() {
            UUID subjectId = UUID.randomUUID();
            CurriculumSubject existing = CurriculumSubject.builder()
                    .id(subjectId)
                    .curriculumId(CURRICULUM_ID)
                    .subjectId(SUBJECT_ID)
                    .semesterNo(1)
                    .courseYear(1)
                    .durationWeeks(16)
                    .assessmentTypeId(ASSESSMENT_TYPE_ID)
                    .build();
            when(curriculumSubjectRepository.findById(subjectId)).thenReturn(Optional.of(existing));
            when(curriculumRepository.findById(CURRICULUM_ID))
                    .thenReturn(Optional.of(curriculum(4)));

            assertThatThrownBy(() -> curriculumSubjectService.updateCurriculumSubject(
                    subjectId, 5, null, null, null, null, null, null, null, null, null, null))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("courseYear must not exceed curriculum duration");
        }
    }
}
