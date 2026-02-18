package com.example.interhubdev.grades.internal;

import com.example.interhubdev.grades.GradeTypeCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for grade_entry ledger row.
 */
@Entity
@Table(name = "grade_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class GradeEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "offering_id", nullable = false)
    private UUID offeringId;

    @Column(name = "points", nullable = false, precision = 6, scale = 2)
    private BigDecimal points;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_code", nullable = false, length = 50)
    private GradeTypeCode typeCode;

    @Column(name = "type_label", length = 255)
    private String typeLabel;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "homework_submission_id")
    private UUID homeworkSubmissionId;

    @Column(name = "graded_by", nullable = false)
    private UUID gradedBy;

    @Column(name = "graded_at", nullable = false)
    private LocalDateTime gradedAt;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_VOIDED = "VOIDED";
}
