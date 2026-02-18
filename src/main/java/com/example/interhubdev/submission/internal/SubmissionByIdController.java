package com.example.interhubdev.submission.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.submission.SubmissionApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for submission by id: get, delete.
 * Get only for teachers and admins; delete only for the author (student).
 */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "Homework submissions", description = "Student solutions for homework. Get: teachers and admins. Delete: author only.")
class SubmissionByIdController {

    private final SubmissionApi submissionApi;
    private final AuthApi authApi;

    @GetMapping("/{submissionId}")
    @Operation(summary = "Get submission", description = "Get a submission by id. Requires TEACHER or ADMIN role.")
    public ResponseEntity<HomeworkSubmissionDto> get(
            @PathVariable UUID submissionId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        HomeworkSubmissionDto dto = submissionApi.get(submissionId, requesterId)
                .orElseThrow(() -> SubmissionErrors.submissionNotFound(submissionId));
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{submissionId}")
    @Operation(summary = "Delete submission", description = "Delete own submission. Only the author (student) can delete.")
    public ResponseEntity<Void> delete(
            @PathVariable UUID submissionId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        submissionApi.delete(submissionId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
