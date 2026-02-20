package com.example.interhubdev.program.internal;

import com.example.interhubdev.error.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ProgramValidation (semester 1–2, course year against curriculum).
 */
@DisplayName("ProgramValidation")
class ProgramValidationTest {

    @Nested
    @DisplayName("validateSemesterNoOneOrTwo")
    class ValidateSemesterNoOneOrTwo {

        @Test
        @DisplayName("throws when semesterNo is 0")
        void throwsWhenZero() {
            assertThatThrownBy(() -> ProgramValidation.validateSemesterNoOneOrTwo(0, "semesterNo"))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("semesterNo must be 1 or 2");
        }

        @Test
        @DisplayName("throws when semesterNo is 3")
        void throwsWhenThree() {
            assertThatThrownBy(() -> ProgramValidation.validateSemesterNoOneOrTwo(3, "semesterNo"))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("semesterNo must be 1 or 2");
        }

        @Test
        @DisplayName("does not throw when semesterNo is 1")
        void acceptsOne() {
            assertThatCode(() -> ProgramValidation.validateSemesterNoOneOrTwo(1, "semesterNo"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("does not throw when semesterNo is 2")
        void acceptsTwo() {
            assertThatCode(() -> ProgramValidation.validateSemesterNoOneOrTwo(2, "semesterNo"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateCourseYearAgainstCurriculum")
    class ValidateCourseYearAgainstCurriculum {

        @Test
        @DisplayName("throws when courseYear is 0")
        void throwsWhenZero() {
            assertThatThrownBy(() -> ProgramValidation.validateCourseYearAgainstCurriculum(2020, 2024, 0))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("courseYear must be at least 1");
        }

        @Test
        @DisplayName("throws when courseYear exceeds endYear - startYear")
        void throwsWhenExceedsDuration() {
            assertThatThrownBy(() -> ProgramValidation.validateCourseYearAgainstCurriculum(2020, 2024, 5))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("courseYear must not exceed curriculum duration")
                    .hasMessageContaining("max is 4");
        }

        @Test
        @DisplayName("does not throw when courseYear is in range")
        void acceptsValidRange() {
            assertThatCode(() -> ProgramValidation.validateCourseYearAgainstCurriculum(2020, 2024, 1))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ProgramValidation.validateCourseYearAgainstCurriculum(2020, 2024, 4))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("when endYear is null allows any courseYear >= 1")
        void whenEndYearNullAllowsLargeCourseYear() {
            assertThatCode(() -> ProgramValidation.validateCourseYearAgainstCurriculum(2020, null, 10))
                    .doesNotThrowAnyException();
        }
    }
}
