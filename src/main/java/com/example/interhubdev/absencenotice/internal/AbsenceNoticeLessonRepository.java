package com.example.interhubdev.absencenotice.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for absence_notice_lesson.
 */
interface AbsenceNoticeLessonRepository extends JpaRepository<AbsenceNoticeLesson, UUID> {

    List<AbsenceNoticeLesson> findByNoticeIdOrderByLessonSessionId(UUID noticeId);

    List<AbsenceNoticeLesson> findByNoticeIdIn(List<UUID> noticeIds);

    @Query("SELECT anl FROM AbsenceNoticeLesson anl WHERE anl.lessonSessionId = :sessionId")
    List<AbsenceNoticeLesson> findByLessonSessionId(@Param("sessionId") UUID sessionId);

    @Query("SELECT anl FROM AbsenceNoticeLesson anl WHERE anl.lessonSessionId IN :sessionIds")
    List<AbsenceNoticeLesson> findByLessonSessionIdIn(@Param("sessionIds") List<UUID> sessionIds);

    @Modifying
    @Query("DELETE FROM AbsenceNoticeLesson anl WHERE anl.noticeId = :noticeId")
    void deleteByNoticeId(@Param("noticeId") UUID noticeId);

    boolean existsByNoticeIdAndLessonSessionId(UUID noticeId, UUID lessonSessionId);
}
