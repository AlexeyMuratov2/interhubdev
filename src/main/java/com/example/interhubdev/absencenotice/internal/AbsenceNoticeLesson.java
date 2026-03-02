package com.example.interhubdev.absencenotice.internal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * JPA entity for absence_notice_lesson. Links an absence notice to a lesson session.
 * One notice can cover multiple lessons.
 */
@Entity
@Table(name = "absence_notice_lesson",
        uniqueConstraints = @UniqueConstraint(columnNames = {"notice_id", "lesson_session_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class AbsenceNoticeLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "notice_id", nullable = false)
    private UUID noticeId;

    @Column(name = "lesson_session_id", nullable = false)
    private UUID lessonSessionId;
}
