package com.example.interhubdev.academic.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    Optional<AcademicYear> findByIsCurrent(boolean isCurrent);

    boolean existsByName(String name);
}
