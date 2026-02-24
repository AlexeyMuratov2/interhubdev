package com.example.interhubdev.document.internal.homework;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LessonHomework junction table entity.
 */
public interface LessonHomeworkRepository extends JpaRepository<LessonHomework, LessonHomework.LessonHomeworkId> {

    /**
     * Find all homework IDs for a given lesson.
     *
     * @param lessonId lesson UUID
     * @return list of homework IDs
     */
    @Query("SELECT lh.homeworkId FROM LessonHomework lh WHERE lh.lessonId = :lessonId")
    List<UUID> findHomeworkIdsByLessonId(@Param("lessonId") UUID lessonId);

    /**
     * Find all homework IDs for lessons in the given set.
     * Single query; empty collection returns empty list.
     *
     * @param lessonIds lesson UUIDs (must not be null)
     * @return list of homework IDs (may contain duplicates if same homework linked to multiple lessons; caller may distinct)
     */
    @Query("SELECT lh.homeworkId FROM LessonHomework lh WHERE lh.lessonId IN :lessonIds")
    List<UUID> findHomeworkIdsByLessonIdIn(@Param("lessonIds") Collection<UUID> lessonIds);

    /**
     * Find lesson ID for a given homework.
     *
     * @param homeworkId homework UUID
     * @return optional lesson ID
     */
    @Query("SELECT lh.lessonId FROM LessonHomework lh WHERE lh.homeworkId = :homeworkId")
    Optional<UUID> findLessonIdByHomeworkId(@Param("homeworkId") UUID homeworkId);

    /**
     * Find LessonHomework by homework ID (should be unique due to constraint).
     *
     * @param homeworkId homework UUID
     * @return optional LessonHomework
     */
    @Query("SELECT lh FROM LessonHomework lh WHERE lh.homeworkId = :homeworkId")
    Optional<LessonHomework> findByHomeworkId(@Param("homeworkId") UUID homeworkId);

    /**
     * Delete all links for a given lesson.
     *
     * @param lessonId lesson UUID
     */
    void deleteByLessonId(UUID lessonId);
}
