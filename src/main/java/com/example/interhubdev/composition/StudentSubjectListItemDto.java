package com.example.interhubdev.composition;

import java.util.UUID;

/**
 * One subject entry for the student dashboard subject list.
 * Includes subject info and the teacher display name (instead of groups as for teacher view).
 */
public record StudentSubjectListItemDto(
    UUID offeringId,
    UUID curriculumSubjectId,
    UUID subjectId,
    String subjectCode,
    String subjectChineseName,
    String subjectEnglishName,
    String departmentName,
    String teacherDisplayName
) {
}
