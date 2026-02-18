package com.example.interhubdev.submission.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repository for HomeworkSubmissionFile (and check if stored file is in use).
 */
interface HomeworkSubmissionFileRepository extends JpaRepository<HomeworkSubmissionFile, HomeworkSubmissionFile.SubmissionFileId> {

    @Query("SELECT COUNT(f) > 0 FROM HomeworkSubmissionFile f WHERE f.storedFileId = :storedFileId")
    boolean existsByStoredFileId(@Param("storedFileId") UUID storedFileId);
}
