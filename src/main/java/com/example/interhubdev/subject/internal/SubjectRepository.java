package com.example.interhubdev.subject.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Subject} entities.
 * Provides lookup by id/code and stable ordering for list operations.
 */
interface SubjectRepository extends JpaRepository<Subject, UUID> {

    Optional<Subject> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Returns all subjects ordered by code ascending for stable, predictable API responses.
     */
    List<Subject> findAllByOrderByCodeAsc();
}
