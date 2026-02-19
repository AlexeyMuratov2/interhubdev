package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AttendanceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for attendance_record.
 * One record per student per lesson session (unique constraint).
 */
@Entity
@Table(name = "attendance_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lesson_session_id", nullable = false)
    private UUID lessonSessionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "minutes_late")
    private Integer minutesLate;

    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    @Column(name = "marked_by", nullable = false)
    private UUID markedBy;

    @Column(name = "marked_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime markedAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "absence_notice_id")
    private UUID absenceNoticeId;

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
