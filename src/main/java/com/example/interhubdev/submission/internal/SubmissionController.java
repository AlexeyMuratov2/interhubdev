package com.example.interhubdev.submission.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.fileasset.FilePolicyKey;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.submission.SubmissionApi;
import com.example.interhubdev.submission.SubmissionsArchiveHandle;
import com.example.interhubdev.web.MultipartUploadSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for submissions: list by homework, create.
 * Submit only for students; list only for teachers and admins.
 */
@RestController
@RequestMapping("/api/homework")
@RequiredArgsConstructor
@Tag(name = "Homework submissions", description = "Student solutions for homework. Submit: students only. List/Get: teachers and admins.")
class SubmissionController {

    private final SubmissionApi submissionApi;
    private final AuthApi authApi;

    @GetMapping("/{homeworkId}/submissions")
    @Operation(summary = "List submissions", description = "List all submissions for a homework. Requires TEACHER or ADMIN role.")
    public ResponseEntity<List<HomeworkSubmissionDto>> listByHomework(
            @PathVariable UUID homeworkId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        List<HomeworkSubmissionDto> list = submissionApi.listByHomework(homeworkId, requesterId);
        return ResponseEntity.ok(list);
    }

    @PostMapping(value = "/{homeworkId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit solution", description = "Create a submission for a homework. Files are optional. Requires STUDENT role.")
    public ResponseEntity<HomeworkSubmissionDto> create(
            @PathVariable UUID homeworkId,
            @Valid @RequestPart("payload") CreateSubmissionRequest body,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        try (var bundle = MultipartUploadSupport.prepareMany(files, requesterId, FilePolicyKey.CONTROLLED_ATTACHMENT)) {
            HomeworkSubmissionDto dto = submissionApi.create(
                    homeworkId,
                    body.description(),
                    bundle.uploads(),
                    requesterId
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        }
    }

    @GetMapping(value = "/{homeworkId}/submissions/archive", produces = "application/zip")
    @Operation(summary = "Download submissions archive", description = "Download a ZIP of all submitted files for this homework. Requires teacher of the lesson or admin/moderator.")
    public void downloadArchive(
            @PathVariable UUID homeworkId,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        SubmissionsArchiveHandle handle = submissionApi.buildSubmissionsArchive(homeworkId, requesterId);
        String filename = handle.getFilename();
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
        handle.writeTo(response.getOutputStream());
        response.getOutputStream().flush();
    }
}
