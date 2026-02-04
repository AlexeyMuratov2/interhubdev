package com.example.interhubdev.program.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CurriculumSubjectAssessmentRepository extends JpaRepository<CurriculumSubjectAssessment, UUID> {

    List<CurriculumSubjectAssessment> findByCurriculumSubjectId(UUID curriculumSubjectId);

    void deleteByCurriculumSubjectId(UUID curriculumSubjectId);
}
