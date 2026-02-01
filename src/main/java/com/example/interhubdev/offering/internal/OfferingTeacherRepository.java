package com.example.interhubdev.offering.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface OfferingTeacherRepository extends JpaRepository<OfferingTeacher, UUID> {

    List<OfferingTeacher> findByOfferingId(UUID offeringId);

    boolean existsByOfferingIdAndTeacherIdAndRole(UUID offeringId, UUID teacherId, String role);
}
