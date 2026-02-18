package com.example.interhubdev.document.internal.lessonMaterial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LessonMaterialFile junction entity.
 */
public interface LessonMaterialFileRepository extends JpaRepository<LessonMaterialFile, LessonMaterialFile.LessonMaterialFileId> {

    /**
     * Find all file links for a lesson material, ordered by sort_order.
     *
     * @param lessonMaterialId lesson material UUID
     * @return list of lesson material file links with stored file loaded
     */
    @Query("SELECT lmf FROM LessonMaterialFile lmf JOIN FETCH lmf.storedFile WHERE lmf.lessonMaterialId = :lessonMaterialId ORDER BY lmf.sortOrder ASC")
    List<LessonMaterialFile> findByLessonMaterialIdOrderBySortOrder(@Param("lessonMaterialId") UUID lessonMaterialId);

    /**
     * Check if any lesson material uses the given stored file.
     */
    @Query("SELECT COUNT(lmf) > 0 FROM LessonMaterialFile lmf WHERE lmf.storedFileId = :storedFileId")
    boolean existsByStoredFileId(@Param("storedFileId") UUID storedFileId);

    /**
     * Count how many lesson material file links use the given stored file (for cleanup check).
     */
    @Query("SELECT COUNT(lmf) FROM LessonMaterialFile lmf WHERE lmf.storedFileId = :storedFileId")
    long countByStoredFileId(@Param("storedFileId") UUID storedFileId);

    /**
     * Find link by material and stored file.
     */
    @Query("SELECT lmf FROM LessonMaterialFile lmf JOIN FETCH lmf.storedFile WHERE lmf.lessonMaterialId = :materialId AND lmf.storedFileId = :storedFileId")
    Optional<LessonMaterialFile> findByLessonMaterialIdAndStoredFileId(
        @Param("materialId") UUID materialId,
        @Param("storedFileId") UUID storedFileId
    );

    /**
     * Delete the link between a lesson material and a stored file.
     */
    @Modifying
    @Query("DELETE FROM LessonMaterialFile lmf WHERE lmf.lessonMaterialId = :materialId AND lmf.storedFileId = :storedFileId")
    void deleteByLessonMaterialIdAndStoredFileId(
        @Param("materialId") UUID materialId,
        @Param("storedFileId") UUID storedFileId
    );

    /**
     * Get max sort_order for a material (for appending new files).
     */
    @Query("SELECT COALESCE(MAX(lmf.sortOrder), -1) FROM LessonMaterialFile lmf WHERE lmf.lessonMaterialId = :materialId")
    int findMaxSortOrderByLessonMaterialId(@Param("materialId") UUID materialId);
}
