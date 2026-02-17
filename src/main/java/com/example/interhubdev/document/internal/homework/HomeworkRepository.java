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

    List<Homework> findByLessonIdOrderByCreatedAtDesc(UUID lessonId);

    /** Whether any homework references the given stored file (for delete-stored-file guard). */
    @Query("SELECT COUNT(h) > 0 FROM Homework h WHERE h.storedFile.id = :storedFileId")
    boolean existsByStoredFileId(@Param("storedFileId") UUID storedFileId);
}
