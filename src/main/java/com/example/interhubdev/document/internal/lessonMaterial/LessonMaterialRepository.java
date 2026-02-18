package com.example.interhubdev.document.internal.lessonMaterial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LessonMaterial entity.
 */
public interface LessonMaterialRepository extends JpaRepository<LessonMaterial, UUID> {

    /**
     * Find all lesson materials for a lesson with files eagerly loaded, ordered by published_at descending.
     *
     * @param lessonId lesson UUID
     * @return list of lesson materials with files, ordered by published_at descending
     */
    @Query("SELECT DISTINCT lm FROM LessonMaterial lm LEFT JOIN FETCH lm.files f LEFT JOIN FETCH f.storedFile WHERE lm.lessonId = :lessonId ORDER BY lm.publishedAt DESC")
    List<LessonMaterial> findByLessonIdOrderByPublishedAtDescWithFiles(@Param("lessonId") UUID lessonId);

    /**
     * Find lesson material by id with files eagerly loaded (for get single).
     *
     * @param materialId lesson material UUID
     * @return optional lesson material with files
     */
    @Query("SELECT DISTINCT lm FROM LessonMaterial lm LEFT JOIN FETCH lm.files f LEFT JOIN FETCH f.storedFile WHERE lm.id = :materialId")
    Optional<LessonMaterial> findByIdWithFiles(@Param("materialId") UUID materialId);
}
