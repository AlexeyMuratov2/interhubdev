package com.example.interhubdev.document.internal.courseMaterial;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.CourseMaterialApi;
import com.example.interhubdev.document.CourseMaterialDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.fileasset.FilePolicyKey;
import com.example.interhubdev.web.MultipartUploadSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for course materials (attach stored file to offering, list).
 * File must be uploaded first via {@code POST /api/documents/upload}; then use this endpoint to attach by stored file id.
 * Delegates to {@link CourseMaterialApi}.
 */
@RestController
@RequestMapping("/api/offerings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course Materials", description = "Course materials management (files linked to group_subject_offering)")
class CourseMaterialController {

    private final CourseMaterialApi courseMaterialApi;
    private final AuthApi authApi;

    /**
     * Attach an already-uploaded file to the offering as a course material.
     * File must be uploaded first via POST /api/documents/upload; pass the returned stored file id here.
     * Requires TEACHER or ADMIN role.
     */
    @PostMapping(value = "/{offeringId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add course material", description = "Attach an already-uploaded file (by stored file id) to the offering. Upload file first via POST /api/documents/upload. Requires TEACHER or ADMIN role.")
    public ResponseEntity<CourseMaterialDto> addMaterial(
            @PathVariable UUID offeringId,
            @Valid @RequestPart("payload") AddCourseMaterialRequest body,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) {
        UUID authorId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        try (var bundle = MultipartUploadSupport.prepareSingle(file, authorId, FilePolicyKey.CONTROLLED_ATTACHMENT)) {
            if (bundle.uploads().isEmpty()) {
                throw Errors.badRequest("File is required");
            }
            CourseMaterialDto dto = courseMaterialApi.createMaterial(
                    offeringId,
                    bundle.uploads().get(0),
                    body.title(),
                    body.description(),
                    authorId
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        }
    }

    /**
     * List all course materials for an offering.
     * Requires authentication.
     */
    @GetMapping("/{offeringId}/materials")
    @Operation(summary = "List course materials", description = "Get all course materials for an offering. Requires authentication.")
    public ResponseEntity<List<CourseMaterialDto>> listMaterials(
            @PathVariable UUID offeringId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        List<CourseMaterialDto> materials = courseMaterialApi.listByOffering(offeringId, requesterId);
        return ResponseEntity.ok(materials);
    }
}
