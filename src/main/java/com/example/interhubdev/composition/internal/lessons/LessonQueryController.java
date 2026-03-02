package com.example.interhubdev.composition.internal.lessons;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.composition.LessonFullDetailsDto;
import com.example.interhubdev.composition.LessonHomeworkSubmissionsDto;
import com.example.interhubdev.composition.LessonQueryApi;
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
 * REST controller for lesson-related composition endpoints.
 */
@RestController
@RequestMapping("/api/composition")
@RequiredArgsConstructor
@Tag(name = "Composition – Lessons", description = "Lesson full details, roster attendance, homework submissions")
class LessonQueryController {

    private final LessonQueryApi lessonQueryApi;
    private final AuthApi authApi;

    @GetMapping("/lessons/{lessonId}/full-details")
    @Operation(summary = "Get full lesson details", description = "Aggregates subject, group, materials, homework, room, teachers, and offering information")
    public ResponseEntity<LessonFullDetailsDto> getLessonFullDetails(
            @PathVariable UUID lessonId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return ResponseEntity.ok(lessonQueryApi.getLessonFullDetails(lessonId, requesterId));
    }

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

        return ResponseEntity.ok(lessonQueryApi.getLessonRosterAttendance(lessonId, requesterId, includeCanceled));
    }

    @GetMapping("/lessons/{lessonId}/homework-submissions")
    @Operation(summary = "Get lesson homework submissions", description = "All students in the lesson's group with their submissions, points, and files per homework assignment")
    public ResponseEntity<LessonHomeworkSubmissionsDto> getLessonHomeworkSubmissions(
            @PathVariable UUID lessonId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return ResponseEntity.ok(lessonQueryApi.getLessonHomeworkSubmissions(lessonId, requesterId));
    }
}
