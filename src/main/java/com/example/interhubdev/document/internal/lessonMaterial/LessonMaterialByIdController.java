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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for lesson material by id: get, delete, add files, remove file.
 * Paths under /api/lessons/{lessonId}/materials for consistency.
 */
@RestController
@RequestMapping("/api/lessons/{lessonId}/materials")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lesson Materials", description = "Materials for a specific lesson")
class LessonMaterialByIdController {

    private final LessonMaterialApi lessonMaterialApi;
    private final AuthApi authApi;

    @GetMapping("/{materialId}")
    @Operation(summary = "Get lesson material", description = "Get a lesson material by id. Requires authentication.")
    public ResponseEntity<LessonMaterialDto> get(
            @PathVariable UUID lessonId,
            @PathVariable UUID materialId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        LessonMaterialDto dto = lessonMaterialApi.get(materialId, requesterId)
                .orElseThrow(() -> LessonMaterialErrors.materialNotFound(materialId));
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{materialId}")
    @Operation(summary = "Delete lesson material", description = "Delete a lesson material and its file links. Stored files are removed from storage if not used elsewhere. Requires material author or ADMIN/MODERATOR role.")
    public ResponseEntity<Void> delete(
            @PathVariable UUID lessonId,
            @PathVariable UUID materialId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        lessonMaterialApi.delete(materialId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{materialId}/files")
    @Operation(summary = "Add files to lesson material", description = "Add stored files to an existing lesson material. Requires material author or ADMIN/MODERATOR role.")
    public ResponseEntity<Void> addFiles(
            @PathVariable UUID lessonId,
            @PathVariable UUID materialId,
            @Valid @RequestBody AddLessonMaterialFilesRequest body,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        lessonMaterialApi.addFiles(materialId, body.storedFileIds(), requesterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{materialId}/files/{storedFileId}")
    @Operation(summary = "Remove file from lesson material", description = "Remove a file from a lesson material. File is deleted from storage if not used elsewhere. Requires material author or ADMIN/MODERATOR role.")
    public ResponseEntity<Void> removeFile(
            @PathVariable UUID lessonId,
            @PathVariable UUID materialId,
            @PathVariable UUID storedFileId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        lessonMaterialApi.removeFile(materialId, storedFileId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
