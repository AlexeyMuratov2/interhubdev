package com.example.interhubdev.document.internal.lessonMaterial;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.LessonMaterialApi;
import com.example.interhubdev.document.LessonMaterialDto;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for lesson materials: list by lesson, create.
 * Delegates to {@link LessonMaterialApi}.
 */
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lesson Materials", description = "Materials for a specific lesson (one lesson many materials, one material many files)")
class LessonMaterialController {

    private final LessonMaterialApi lessonMaterialApi;
    private final AuthApi authApi;

    @GetMapping("/{lessonId}/materials")
    @Operation(summary = "List lesson materials", description = "List all materials for a lesson. Requires authentication.")
    public ResponseEntity<List<LessonMaterialDto>> listByLesson(
            @PathVariable UUID lessonId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        List<LessonMaterialDto> list = lessonMaterialApi.listByLesson(lessonId, requesterId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{lessonId}/materials")
    @Operation(summary = "Create lesson material", description = "Create a material for a lesson. Optionally attach stored file IDs (upload first via POST /api/documents/upload). Requires TEACHER or ADMIN role.")
    public ResponseEntity<LessonMaterialDto> create(
            @PathVariable UUID lessonId,
            @Valid @RequestBody CreateLessonMaterialRequest body,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        LessonMaterialDto dto = lessonMaterialApi.create(
                lessonId,
                body.name(),
                body.description(),
                requesterId,
                body.publishedAt(),
                body.storedFileIds()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
