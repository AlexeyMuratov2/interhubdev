package com.example.interhubdev.subject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for teacher subject detail (full information).
 * Contains subject data, curriculum subject data, assessments, and all offerings with materials.
 */
public record TeacherSubjectDetailDto(
    // Данные предмета (одинаковые для всех реализаций)
    SubjectInfoDto subject,
    
    // Данные curriculum_subject по конкретному семестру
    CurriculumSubjectInfoDto curriculumSubject,
    
    // Оценки для этого предмета
    List<CurriculumSubjectAssessmentInfoDto> assessments,
    
    // Реализации для разных групп (offerings)
    List<GroupSubjectOfferingInfoDto> offerings
) {
    /**
     * Subject information (same for all implementations).
     */
    public record SubjectInfoDto(
        UUID id,
        String code,
        String chineseName,
        String englishName,
        String description,
        UUID departmentId,
        String departmentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}
    
    /**
     * Curriculum subject information for specific semester.
     */
    public record CurriculumSubjectInfoDto(
        UUID id,
        UUID curriculumId,
        UUID subjectId,
        int semesterNo,
        Integer courseYear,
        int durationWeeks,
        Integer hoursTotal,
        Integer hoursLecture,
        Integer hoursPractice,
        Integer hoursLab,
        Integer hoursSeminar,
        Integer hoursSelfStudy,
        Integer hoursConsultation,
        Integer hoursCourseWork,
        UUID assessmentTypeId,
        String assessmentTypeName,
        BigDecimal credits,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}
    
    /**
     * Curriculum subject assessment information.
     */
    public record CurriculumSubjectAssessmentInfoDto(
        UUID id,
        UUID assessmentTypeId,
        String assessmentTypeName,
        Integer weekNumber,
        boolean isFinal,
        BigDecimal weight,
        String notes,
        LocalDateTime createdAt
    ) {}
    
    /**
     * Group subject offering information with materials.
     */
    public record GroupSubjectOfferingInfoDto(
        UUID id,
        UUID groupId,
        String groupCode,
        String groupName,
        UUID teacherId,
        UUID roomId,
        String roomName,
        String format,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CourseMaterialInfoDto> materials
    ) {}
    
    /**
     * Course material information.
     */
    public record CourseMaterialInfoDto(
        UUID id,
        String title,
        String description,
        UUID authorId,
        String authorName,
        LocalDateTime uploadedAt,
        StoredFileInfoDto file
    ) {}
    
    /**
     * Stored file information.
     */
    public record StoredFileInfoDto(
        UUID id,
        String originalName,
        String contentType,
        long size,
        LocalDateTime uploadedAt,
        UUID uploadedBy
    ) {}
}
