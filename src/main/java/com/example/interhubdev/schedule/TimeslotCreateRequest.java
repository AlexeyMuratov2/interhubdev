package com.example.interhubdev.schedule;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a single timeslot or one item in bulk.
 * startTime and endTime: HH:mm or HH:mm:ss. endTime must be after startTime.
 */
public record TimeslotCreateRequest(
    @Min(value = 1, message = "dayOfWeek must be 1..7") @Max(value = 7, message = "dayOfWeek must be 1..7") int dayOfWeek,
    @NotBlank(message = "startTime is required") String startTime,
    @NotBlank(message = "endTime is required") String endTime
) {
}
