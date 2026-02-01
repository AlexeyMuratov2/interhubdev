package com.example.interhubdev.subject.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface AssessmentTypeRepository extends JpaRepository<AssessmentType, UUID> {

    Optional<AssessmentType> findByCode(String code);

    boolean existsByCode(String code);
}
