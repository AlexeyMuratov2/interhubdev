package com.example.interhubdev.program.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CurriculumSubjectRepository extends JpaRepository<CurriculumSubject, UUID> {

    List<CurriculumSubject> findByCurriculumId(UUID curriculumId);

    /** Subjects of a curriculum ordered by semester and subject for stable rendering. */
    List<CurriculumSubject> findByCurriculumIdOrderBySemesterNoAscSubjectIdAsc(UUID curriculumId);

    Optional<CurriculumSubject> findByCurriculumIdAndSubjectIdAndSemesterNo(
            UUID curriculumId, UUID subjectId, int semesterNo);

    boolean existsByCurriculumIdAndSubjectIdAndSemesterNo(
            UUID curriculumId, UUID subjectId, int semesterNo);
}
