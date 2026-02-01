package com.example.interhubdev.program.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ProgramRepository extends JpaRepository<Program, UUID> {

    Optional<Program> findByCode(String code);

    boolean existsByCode(String code);
}
