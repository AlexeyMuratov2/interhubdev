package com.example.interhubdev.program.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CurriculumPracticeRepository extends JpaRepository<CurriculumPractice, UUID> {

    List<CurriculumPractice> findByCurriculumId(UUID curriculumId);

    /** Practices for the given curriculum ordered by semester and name. */
    List<CurriculumPractice> findByCurriculumIdOrderBySemesterNoAscNameAsc(UUID curriculumId);
}
