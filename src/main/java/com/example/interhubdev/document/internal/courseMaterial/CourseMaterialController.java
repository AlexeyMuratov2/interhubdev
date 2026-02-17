package com.example.interhubdev.document.internal.courseMaterial;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.CourseMaterialApi;
import com.example.interhubdev.document.CourseMaterialDto;
import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for course materials (upload, list).
 * Delegates to {@link CourseMaterialApi}.
 */
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course Materials", description = "Course materials management (files linked to subjects)")
class CourseMaterialController {

    private final CourseMaterialApi courseMaterialApi;
    private final AuthApi authApi;

    /**
     * Upload a file and create a course material.
     * Requires TEACHER or ADMIN role.
     */
    @PostMapping("/{subjectId}/materials/upload")
    @Operation(summary = "Upload course material", description = "Upload a file and create a course material. Requires TEACHER or ADMIN role.")
    public ResponseEntity<CourseMaterialDto> uploadMaterial(
            @PathVariable UUID subjectId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            HttpServletRequest request
    ) {
        UUID authorId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        if (file.isEmpty()) {
            throw Errors.badRequest("File is empty");
        }

        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        CourseMaterialDto dto;
        try (InputStream stream = file.getInputStream()) {
            dto = courseMaterialApi.uploadMaterial(
                    subjectId,
                    stream,
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "file",
                    contentType,
                    file.getSize(),
                    title,
                    description,
                    authorId
            );
        } catch (AppException e) {
            // Re-throw AppException as-is (already properly formatted)
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during course material upload", e);
            throw Errors.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Failed to upload course material. Please try again.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * List all course materials for a subject.
     * Requires authentication.
     */
    @GetMapping("/{subjectId}/materials")
    @Operation(summary = "List course materials", description = "Get all course materials for a subject. Requires authentication.")
    public ResponseEntity<List<CourseMaterialDto>> listMaterials(
            @PathVariable UUID subjectId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        List<CourseMaterialDto> materials = courseMaterialApi.listBySubject(subjectId, requesterId);
        return ResponseEntity.ok(materials);
    }
}
