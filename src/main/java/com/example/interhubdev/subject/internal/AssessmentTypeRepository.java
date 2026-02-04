package com.example.interhubdev.subject.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link AssessmentType} entities.
 * Provides lookup by id/code and stable ordering by sort order then code.
 */
interface AssessmentTypeRepository extends JpaRepository<AssessmentType, UUID> {

    Optional<AssessmentType> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Returns all assessment types ordered by sort order ascending, then code ascending,
     * for stable, predictable API responses (e.g. dropdowns).
     */
    List<AssessmentType> findAllByOrderBySortOrderAscCodeAsc();
}
