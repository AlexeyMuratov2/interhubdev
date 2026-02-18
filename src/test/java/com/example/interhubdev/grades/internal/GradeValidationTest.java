package com.example.interhubdev.grades.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.grades.GradeTypeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for GradeValidation (CUSTOM type_label, points range, description length).
 */
@DisplayName("GradeValidation")
class GradeValidationTest {

    @Nested
    @DisplayName("validateTypeAndLabel")
    class ValidateTypeAndLabel {

        @Test
        @DisplayName("CUSTOM with null typeLabel throws")
        void customNullLabel() {
            assertThatThrownBy(() -> GradeValidation.validateTypeAndLabel(GradeTypeCode.CUSTOM, null))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("typeLabel is required when typeCode is CUSTOM");
        }

        @Test
        @DisplayName("CUSTOM with blank typeLabel throws")
        void customBlankLabel() {
            assertThatThrownBy(() -> GradeValidation.validateTypeAndLabel(GradeTypeCode.CUSTOM, "   "))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("typeLabel is required");
        }

        @Test
        @DisplayName("CUSTOM with valid typeLabel does not throw")
        void customValidLabel() {
            GradeValidation.validateTypeAndLabel(GradeTypeCode.CUSTOM, "Quiz");
        }

        @Test
        @DisplayName("non-CUSTOM with non-null typeLabel throws")
        void nonCustomWithLabel() {
            assertThatThrownBy(() -> GradeValidation.validateTypeAndLabel(GradeTypeCode.SEMINAR, "Extra"))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("typeLabel must be null when typeCode is not CUSTOM");
        }

        @Test
        @DisplayName("non-CUSTOM with null typeLabel does not throw")
        void nonCustomNullLabel() {
            GradeValidation.validateTypeAndLabel(GradeTypeCode.HOMEWORK, null);
        }
    }

    @Nested
    @DisplayName("validatePoints")
    class ValidatePoints {

        @Test
        @DisplayName("null points throws")
        void nullPoints() {
            assertThatThrownBy(() -> GradeValidation.validatePoints(null))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("points is required");
        }

        @Test
        @DisplayName("points below -9999.99 throws")
        void pointsTooLow() {
            assertThatThrownBy(() -> GradeValidation.validatePoints(new BigDecimal("-10000")))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("points must be between");
        }

        @Test
        @DisplayName("points above 9999.99 throws")
        void pointsTooHigh() {
            assertThatThrownBy(() -> GradeValidation.validatePoints(new BigDecimal("10000")))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("points must be between");
        }

        @Test
        @DisplayName("points in range does not throw")
        void pointsInRange() {
            GradeValidation.validatePoints(BigDecimal.ZERO);
            GradeValidation.validatePoints(new BigDecimal("100.5"));
            GradeValidation.validatePoints(new BigDecimal("-100.5"));
        }
    }

    @Nested
    @DisplayName("validateDescription")
    class ValidateDescription {

        @Test
        @DisplayName("description over 2000 chars throws")
        void descriptionTooLong() {
            String longDesc = "x".repeat(2001);
            assertThatThrownBy(() -> GradeValidation.validateDescription(longDesc))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("description must not exceed");
        }

        @Test
        @DisplayName("null or short description does not throw")
        void descriptionOk() {
            GradeValidation.validateDescription(null);
            GradeValidation.validateDescription("Short");
        }
    }
}
