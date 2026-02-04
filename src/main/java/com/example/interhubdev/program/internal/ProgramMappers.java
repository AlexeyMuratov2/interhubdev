package com.example.interhubdev.program.internal;

import com.example.interhubdev.program.CurriculumDto;
import com.example.interhubdev.program.CurriculumPracticeDto;
import com.example.interhubdev.program.CurriculumSubjectAssessmentDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.ProgramDto;

/**
 * Internal mappers for Program module entities to DTOs.
 */
final class ProgramMappers {

    private ProgramMappers() {
    }

    static ProgramDto toProgramDto(Program e) {
        return new ProgramDto(e.getId(), e.getCode(), e.getName(), e.getDescription(),
                e.getDegreeLevel(), e.getDepartmentId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    static CurriculumDto toCurriculumDto(Curriculum e) {
        return new CurriculumDto(e.getId(), e.getProgramId(), e.getVersion(), e.getStartYear(),
                e.getEndYear(), e.isActive(), e.getStatus(), e.getApprovedAt(), e.getApprovedBy(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }

    static CurriculumSubjectDto toCurriculumSubjectDto(CurriculumSubject e) {
        return new CurriculumSubjectDto(e.getId(), e.getCurriculumId(), e.getSubjectId(), e.getSemesterNo(),
                e.getCourseYear(), e.getDurationWeeks(), e.getHoursTotal(), e.getHoursLecture(),
                e.getHoursPractice(), e.getHoursLab(), e.getHoursSeminar(), e.getHoursSelfStudy(),
                e.getHoursConsultation(), e.getHoursCourseWork(), e.getAssessmentTypeId(),
                e.getCredits(), e.getCreatedAt(), e.getUpdatedAt());
    }

    static CurriculumSubjectAssessmentDto toCurriculumSubjectAssessmentDto(CurriculumSubjectAssessment e) {
        return new CurriculumSubjectAssessmentDto(e.getId(), e.getCurriculumSubjectId(), e.getAssessmentTypeId(),
                e.getWeekNumber(), e.isFinal(), e.getWeight(), e.getNotes(), e.getCreatedAt());
    }

    static CurriculumPracticeDto toCurriculumPracticeDto(CurriculumPractice e) {
        return new CurriculumPracticeDto(e.getId(), e.getCurriculumId(), e.getPracticeType(), e.getName(),
                e.getDescription(), e.getSemesterNo(), e.getDurationWeeks(), e.getCredits(),
                e.getAssessmentTypeId(), e.getLocationType(), e.isSupervisorRequired(),
                e.isReportRequired(), e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
}

