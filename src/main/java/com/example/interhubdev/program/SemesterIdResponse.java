package com.example.interhubdev.program;

import java.util.UUID;

/**
 * Response containing the semester ID resolved from curriculum, course and semester number.
 */
public record SemesterIdResponse(UUID semesterId) {
}
