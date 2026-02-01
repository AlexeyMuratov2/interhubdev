package com.example.interhubdev.group;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record GroupLeaderDto(
    UUID id,
    UUID groupId,
    UUID studentId,
    String role,
    LocalDate fromDate,
    LocalDate toDate,
    LocalDateTime createdAt
) {
}
