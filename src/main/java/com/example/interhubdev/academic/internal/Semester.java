package com.example.interhubdev.academic.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "semester")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "exam_start_date")
    private LocalDate examStartDate;

    @Column(name = "exam_end_date")
    private LocalDate examEndDate;

    @Column(name = "week_count")
    @Builder.Default
    private Integer weekCount = 16;

    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private boolean isCurrent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
