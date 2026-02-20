package com.example.interhubdev.composition.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.composition.CompositionApi;
import com.example.interhubdev.composition.LessonFullDetailsDto;
import com.example.interhubdev.composition.LessonRosterAttendanceDto;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for composition endpoints.
 * Thin layer: only handles HTTP, delegates to CompositionApi.
 */
@RestController
@RequestMapping("/api/composition")
@RequiredArgsConstructor
@Tag(name = "Composition", description = "Read-only data aggregation for complex UI screens")
class CompositionController {

    private final CompositionApi compositionApi;
    private final AuthApi authApi;

    /**
     * Get full details for a lesson (Use Case #1: Lesson Full Details).
     * Aggregates all data needed for the "Full Lesson Information" screen.
     *
     * @param lessonId lesson ID (path variable)
     * @param request HTTP request (for authentication)
     * @return 200 OK with LessonFullDetailsDto
     */
    @GetMapping("/lessons/{lessonId}/full-details")
    @Operation(summary = "Get full lesson details", description = "Aggregates subject, group, materials, homework, room, teachers, and offering information")
    public ResponseEntity<LessonFullDetailsDto> getLessonFullDetails(
            @PathVariable UUID lessonId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        LessonFullDetailsDto details = compositionApi.getLessonFullDetails(lessonId, requesterId);
        return ResponseEntity.ok(details);
    }

    /**
     * Get roster attendance for a lesson: all students in the group with attendance status and absence notices.
     * For the lesson screen attendance table UI.
     *
     * @param lessonId       lesson (session) ID
     * @param includeCanceled if true, include CANCELED absence notices in response
     * @param request        HTTP request (for authentication)
     * @return 200 OK with LessonRosterAttendanceDto
     */
    @GetMapping("/lessons/{lessonId}/roster-attendance")
    @Operation(summary = "Get lesson roster attendance", description = "All students in the lesson's group with attendance status and absence notices for this lesson")
    public ResponseEntity<LessonRosterAttendanceDto> getLessonRosterAttendance(
            @PathVariable UUID lessonId,
            @RequestParam(defaultValue = "false") boolean includeCanceled,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        LessonRosterAttendanceDto dto = compositionApi.getLessonRosterAttendance(lessonId, requesterId, includeCanceled);
        return ResponseEntity.ok(dto);
    }
}
