package com.example.interhubdev.offering;

import java.time.LocalDateTime;
import java.util.UUID;

public record OfferingTeacherDto(
    UUID id,
    UUID offeringId,
    UUID teacherId,
    String role,
    LocalDateTime createdAt
) {
}
