package com.example.interhubdev.grades.internal;

import com.example.interhubdev.grades.GradeTypeCode;

/**
 * Business validation for grade entries. Throws {@link com.example.interhubdev.error.AppException} via GradeErrors.
 */
final class GradeValidation {

    private static final int MAX_TYPE_LABEL_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    private GradeValidation() {
    }

    /**
     * Validate typeCode and typeLabel: CUSTOM requires non-blank typeLabel; non-CUSTOM must have null typeLabel.
     */
    static void validateTypeAndLabel(GradeTypeCode typeCode, String typeLabel) {
        if (typeCode == GradeTypeCode.CUSTOM) {
            if (typeLabel == null || typeLabel.isBlank()) {
                throw GradeErrors.validationFailed("typeLabel is required when typeCode is CUSTOM");
            }
            if (typeLabel.length() > MAX_TYPE_LABEL_LENGTH) {
                throw GradeErrors.validationFailed("typeLabel must not exceed " + MAX_TYPE_LABEL_LENGTH + " characters");
            }
        } else {
            if (typeLabel != null && !typeLabel.isBlank()) {
                throw GradeErrors.validationFailed("typeLabel must be null when typeCode is not CUSTOM");
            }
        }
    }

    static void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw GradeErrors.validationFailed("description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
    }

    static void validatePoints(java.math.BigDecimal points) {
        if (points == null) {
            throw GradeErrors.validationFailed("points is required");
        }
        if (points.compareTo(new java.math.BigDecimal("-9999.99")) < 0
                || points.compareTo(new java.math.BigDecimal("9999.99")) > 0) {
            throw GradeErrors.validationFailed("points must be between -9999.99 and 9999.99");
        }
    }
}
