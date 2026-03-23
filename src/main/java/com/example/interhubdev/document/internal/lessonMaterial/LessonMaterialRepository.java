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

    @Query("SELECT lm FROM LessonMaterial lm WHERE lm.lessonId = :lessonId ORDER BY lm.publishedAt DESC")
    List<LessonMaterial> findByLessonIdOrderByPublishedAtDescWithFiles(@Param("lessonId") UUID lessonId);

    @Query("SELECT lm FROM LessonMaterial lm WHERE lm.id = :materialId")
    Optional<LessonMaterial> findByIdWithFiles(@Param("materialId") UUID materialId);
}
