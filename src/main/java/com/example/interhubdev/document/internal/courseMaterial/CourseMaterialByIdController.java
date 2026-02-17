package com.example.interhubdev.document.internal.courseMaterial;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.CourseMaterialApi;
import com.example.interhubdev.document.CourseMaterialDto;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for course material operations by material ID (get, delete).
 * Delegates to {@link CourseMaterialApi}.
 */
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course Materials", description = "Course materials management (files linked to subjects)")
class CourseMaterialByIdController {

    private final CourseMaterialApi courseMaterialApi;
    private final AuthApi authApi;

    /**
     * Get a course material by id.
     * Requires authentication.
     */
    @GetMapping("/{materialId}")
    @Operation(summary = "Get course material", description = "Get a course material by id. Requires authentication.")
    public ResponseEntity<CourseMaterialDto> getMaterial(
            @PathVariable UUID materialId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        CourseMaterialDto dto = courseMaterialApi.get(materialId, requesterId)
                .orElseThrow(() -> CourseMaterialErrors.materialNotFound(materialId));
        return ResponseEntity.ok(dto);
    }

    /**
     * Delete a course material.
     * Requires permission: material author or ADMIN/MODERATOR role.
     */
    @DeleteMapping("/{materialId}")
    @Operation(summary = "Delete course material", description = "Delete a course material. Requires permission: material author or ADMIN/MODERATOR role.")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable UUID materialId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        courseMaterialApi.delete(materialId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
