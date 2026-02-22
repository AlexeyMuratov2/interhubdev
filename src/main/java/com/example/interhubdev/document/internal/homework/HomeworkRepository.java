package com.example.interhubdev.document.internal.homework;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Homework entity.
 */
public interface HomeworkRepository extends JpaRepository<Homework, UUID> {

    /**
     * Find all homework for a given lesson (via junction table).
     * Uses JOIN FETCH to eagerly load lessonHomework relationship.
     *
     * @param lessonId lesson UUID
     * @return list of homework entities ordered by creation date descending
     */
    @Query("SELECT DISTINCT h FROM Homework h JOIN FETCH h.lessonHomework lh WHERE lh.lessonId = :lessonId ORDER BY h.createdAt DESC")
    List<Homework> findByLessonIdOrderByCreatedAtDesc(@Param("lessonId") UUID lessonId);
    
    /**
     * Find homework by ID with lessonHomework relationship loaded.
     *
     * @param homeworkId homework UUID
     * @return optional homework entity with lessonHomework loaded
     */
    @Query("SELECT h FROM Homework h LEFT JOIN FETCH h.lessonHomework WHERE h.id = :homeworkId")
    java.util.Optional<Homework> findByIdWithLesson(@Param("homeworkId") UUID homeworkId);

    /**
     * Find homework by ID with lessonHomework and files (with storedFile) loaded.
     */
    @Query("SELECT DISTINCT h FROM Homework h LEFT JOIN FETCH h.lessonHomework LEFT JOIN FETCH h.files f LEFT JOIN FETCH f.storedFile WHERE h.id = :homeworkId")
    java.util.Optional<Homework> findByIdWithLessonAndFiles(@Param("homeworkId") UUID homeworkId);

    /**
     * Find all homework for a lesson with files loaded.
     */
    @Query("SELECT DISTINCT h FROM Homework h JOIN FETCH h.lessonHomework lh LEFT JOIN FETCH h.files f LEFT JOIN FETCH f.storedFile WHERE lh.lessonId = :lessonId ORDER BY h.createdAt DESC")
    List<Homework> findByLessonIdOrderByCreatedAtDescWithFiles(@Param("lessonId") UUID lessonId);
}
