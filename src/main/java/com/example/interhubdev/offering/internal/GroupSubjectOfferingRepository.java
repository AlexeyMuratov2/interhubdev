package com.example.interhubdev.offering.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface GroupSubjectOfferingRepository extends JpaRepository<GroupSubjectOffering, UUID> {

    List<GroupSubjectOffering> findByGroupIdOrderByCurriculumSubjectIdAsc(UUID groupId);

    Optional<GroupSubjectOffering> findByGroupIdAndCurriculumSubjectId(UUID groupId, UUID curriculumSubjectId);

    boolean existsByGroupIdAndCurriculumSubjectId(UUID groupId, UUID curriculumSubjectId);

    List<GroupSubjectOffering> findByTeacherId(UUID teacherId);
}
