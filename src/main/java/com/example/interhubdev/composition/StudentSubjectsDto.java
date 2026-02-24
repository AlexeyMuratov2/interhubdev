package com.example.interhubdev.composition;

import java.util.List;

/**
 * Response for GET /api/composition/student/subjects.
 * Lists all subjects for which the current student has at least one lesson,
 * with subject info and teacher display name per offering.
 */
public record StudentSubjectsDto(
    List<StudentSubjectListItemDto> items
) {
}
