package com.example.interhubdev.department;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Department.
 */
public record DepartmentDto(
    UUID id,
    String code,
    String name,
    String description,
    LocalDateTime createdAt
) {
}
