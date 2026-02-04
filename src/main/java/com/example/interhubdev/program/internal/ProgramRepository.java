package com.example.interhubdev.program.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProgramRepository extends JpaRepository<Program, UUID> {

    Optional<Program> findByCode(String code);

    boolean existsByCode(String code);

    /** Programs ordered by code ascending for stable UI ordering. */
    List<Program> findAllByOrderByCodeAsc();
}
