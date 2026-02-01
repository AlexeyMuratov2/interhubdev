package com.example.interhubdev.program.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CurriculumRepository extends JpaRepository<Curriculum, UUID> {

    List<Curriculum> findByProgramId(UUID programId);

    boolean existsByProgramIdAndVersion(UUID programId, String version);
}
