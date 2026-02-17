package com.example.interhubdev.document.internal.courseMaterial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CourseMaterial entity.
 */
public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, UUID> {

    /**
     * Find all course materials for a subject, ordered by uploaded_at descending.
     */
    List<CourseMaterial> findBySubjectIdOrderByUploadedAtDesc(UUID subjectId);

    /**
     * Check if any course material uses the given stored file.
     */
    @Query("SELECT COUNT(c) > 0 FROM CourseMaterial c WHERE c.storedFile.id = :storedFileId")
    boolean existsByStoredFileId(@Param("storedFileId") UUID storedFileId);

    /**
     * Count how many course materials use the given stored file.
     */
    @Query("SELECT COUNT(c) FROM CourseMaterial c WHERE c.storedFile.id = :storedFileId")
    long countByStoredFileId(@Param("storedFileId") UUID storedFileId);
}
