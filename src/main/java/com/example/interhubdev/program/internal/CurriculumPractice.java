package com.example.interhubdev.program.internal;

import com.example.interhubdev.program.PracticeLocation;
import com.example.interhubdev.program.PracticeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "curriculum_practice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class CurriculumPractice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "curriculum_id", nullable = false)
    private UUID curriculumId;

    @Enumerated(EnumType.STRING)
    @Column(name = "practice_type", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PracticeType practiceType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "semester_no", nullable = false)
    private int semesterNo;

    @Column(name = "duration_weeks", nullable = false)
    private int durationWeeks;

    @Column(name = "credits", precision = 5, scale = 2)
    private BigDecimal credits;

    @Column(name = "assessment_type_id")
    private UUID assessmentTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private PracticeLocation locationType = PracticeLocation.ENTERPRISE;

    @Column(name = "supervisor_required", nullable = false)
    @Builder.Default
    private boolean supervisorRequired = true;

    @Column(name = "report_required", nullable = false)
    @Builder.Default
    private boolean reportRequired = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
