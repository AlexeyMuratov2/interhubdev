package com.example.interhubdev.submission.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.submission.SubmissionApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final DocumentApi documentApi;
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

    @PostMapping("/{homeworkId}/submissions")
    @Operation(summary = "Submit solution", description = "Create a submission for a homework. Files are optional. Requires STUDENT role.")
    public ResponseEntity<HomeworkSubmissionDto> create(
            @PathVariable UUID homeworkId,
            @Valid @RequestBody CreateSubmissionRequest body,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        List<UUID> fileIds = body.storedFileIds() != null ? body.storedFileIds() : List.of();
        for (UUID fileId : fileIds) {
            if (documentApi.getStoredFile(fileId).isEmpty()) {
                throw SubmissionErrors.fileNotFound(fileId);
            }
        }
        HomeworkSubmissionDto dto = submissionApi.create(
                homeworkId,
                body.description(),
                fileIds,
                requesterId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
