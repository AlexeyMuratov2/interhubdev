package com.example.interhubdev.document.internal.courseMaterial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for CourseMaterial entity.
 */
public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, UUID> {

    /**
     * Find all course materials for an offering, ordered by uploaded_at descending.
     *
     * @param offeringId group subject offering UUID
     * @return list of course materials ordered by uploaded_at descending
     */
    List<CourseMaterial> findByOfferingIdOrderByUploadedAtDesc(UUID offeringId);

}
