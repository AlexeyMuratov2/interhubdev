package com.example.interhubdev.document.internal.lessonMaterial;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.LessonMaterialApi;
import com.example.interhubdev.document.LessonMaterialDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.fileasset.FilePolicyKey;
import com.example.interhubdev.web.MultipartUploadSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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

    @PostMapping(value = "/{materialId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add files to lesson material", description = "Add stored files to an existing lesson material. Requires material author or ADMIN/MODERATOR role.")
    public ResponseEntity<Void> addFiles(
            @PathVariable UUID lessonId,
            @PathVariable UUID materialId,
            @RequestPart("files") List<MultipartFile> files,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        try (var bundle = MultipartUploadSupport.prepareMany(files, requesterId, FilePolicyKey.CONTROLLED_ATTACHMENT)) {
            lessonMaterialApi.addFiles(materialId, bundle.uploads(), requesterId);
            return ResponseEntity.noContent().build();
        }
    }

    @DeleteMapping("/{materialId}/attachments/{attachmentId}")
    @Operation(summary = "Remove file from lesson material", description = "Remove a file from a lesson material. File is deleted from storage if not used elsewhere. Requires material author or ADMIN/MODERATOR role.")
    public ResponseEntity<Void> removeFile(
            @PathVariable UUID lessonId,
            @PathVariable UUID materialId,
            @PathVariable UUID attachmentId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        lessonMaterialApi.removeFile(materialId, attachmentId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
