package com.example.interhubdev.document.internal.homework;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repository for HomeworkFile junction entity.
 */
public interface HomeworkFileRepository extends JpaRepository<HomeworkFile, HomeworkFile.HomeworkFileId> {

    /**
     * Whether any homework file link references the given stored file (for delete-stored-file guard).
     */
    @Query("SELECT COUNT(hf) > 0 FROM HomeworkFile hf WHERE hf.storedFileId = :storedFileId")
    boolean existsByStoredFileId(@Param("storedFileId") UUID storedFileId);
}
