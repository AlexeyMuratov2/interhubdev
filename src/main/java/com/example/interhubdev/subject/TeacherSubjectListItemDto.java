package com.example.interhubdev.subject;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for teacher subject list item (shortened view).
 * Contains basic subject information and groups where teacher teaches this subject.
 */
public record TeacherSubjectListItemDto(
    UUID curriculumSubjectId,
    UUID subjectId,
    String subjectCode,
    String subjectChineseName,
    String subjectEnglishName,
    String subjectDescription,
    UUID departmentId,
    String departmentName,
    int semesterNo,
    Integer courseYear,
    int durationWeeks,
    UUID assessmentTypeId,
    String assessmentTypeName,
    BigDecimal credits,
    List<GroupInfoDto> groups
) {
    /**
     * Group information for teacher subject list.
     */
    public record GroupInfoDto(
        UUID id,
        String code,
        String name
    ) {}
}
