package com.example.interhubdev.subject;

import java.time.LocalDateTime;
import java.util.UUID;

public record AssessmentTypeDto(
    UUID id,
    String code,
    String name,
    LocalDateTime createdAt
) {
}
