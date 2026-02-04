package com.example.interhubdev.subject;

import java.time.LocalDateTime;
import java.util.UUID;

public record AssessmentTypeDto(
    UUID id,
    String code,
    String chineseName,
    String englishName,
    Boolean isGraded,
    Boolean isFinal,
    Integer sortOrder,
    LocalDateTime createdAt
) {
}
