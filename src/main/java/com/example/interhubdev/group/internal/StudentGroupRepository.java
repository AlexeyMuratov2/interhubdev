package com.example.interhubdev.group.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StudentGroupRepository extends JpaRepository<StudentGroup, UUID> {

    Optional<StudentGroup> findByCode(String code);

    List<StudentGroup> findByProgramId(UUID programId);

    boolean existsByCode(String code);
}
