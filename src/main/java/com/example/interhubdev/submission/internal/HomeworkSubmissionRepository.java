package com.example.interhubdev.submission.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for HomeworkSubmission entity.
 */
interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, UUID> {

    @Query("SELECT DISTINCT s FROM HomeworkSubmission s LEFT JOIN FETCH s.files WHERE s.homeworkId = :homeworkId ORDER BY s.submittedAt DESC")
    List<HomeworkSubmission> findByHomeworkIdOrderBySubmittedAtDesc(@Param("homeworkId") UUID homeworkId);

    @Query("SELECT s FROM HomeworkSubmission s LEFT JOIN FETCH s.files WHERE s.id = :id")
    java.util.Optional<HomeworkSubmission> findByIdWithFiles(@Param("id") UUID id);

    /**
     * Find submissions by IDs with files loaded. Single batch query; no N+1.
     */
    @Query("SELECT DISTINCT s FROM HomeworkSubmission s LEFT JOIN FETCH s.files WHERE s.id IN :ids")
    List<HomeworkSubmission> findByIdInWithFiles(@Param("ids") Collection<UUID> ids);

    /**
     * Find all submissions by a given author for a homework (at most one expected after replace-on-create policy).
     */
    List<HomeworkSubmission> findByHomeworkIdAndAuthorId(UUID homeworkId, UUID authorId);

    /**
     * Find all submissions for any of the given homework IDs. Single batch query; no N+1.
     *
     * @param homeworkIds homework UUIDs (must not be null; empty returns empty list)
     * @return list of submissions (order not guaranteed)
     */
    @Query("SELECT s FROM HomeworkSubmission s LEFT JOIN FETCH s.files WHERE s.homeworkId IN :homeworkIds")
    List<HomeworkSubmission> findByHomeworkIdIn(@Param("homeworkIds") Collection<UUID> homeworkIds);

    /**
     * Count distinct homework IDs for which the given author has at least one submission.
     * Used for student "submitted homework count" stat.
     */
    @Query("SELECT COUNT(DISTINCT s.homeworkId) FROM HomeworkSubmission s WHERE s.authorId = :authorId AND s.homeworkId IN :homeworkIds")
    long countDistinctHomeworkIdsByAuthorIdAndHomeworkIdIn(@Param("authorId") UUID authorId, @Param("homeworkIds") Collection<UUID> homeworkIds);
}
