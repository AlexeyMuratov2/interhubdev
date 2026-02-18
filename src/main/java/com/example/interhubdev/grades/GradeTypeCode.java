package com.example.interhubdev.grades;

/**
 * System codes for grade allocation type.
 * CUSTOM requires a separate type_label (teacher-defined name).
 */
public enum GradeTypeCode {
    SEMINAR,
    EXAM,
    COURSEWORK,
    HOMEWORK,
    OTHER,
    /** Teacher-defined type; type_label must be provided. */
    CUSTOM
}
