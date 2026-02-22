package com.example.interhubdev.document.internal.homework;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.HomeworkDto;
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
 * REST controller for homework by id: get, update, delete.
 * Delegates to {@link HomeworkApi}.
 */
@RestController
@RequestMapping("/api/homework")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Homework", description = "Homework assignments linked to lessons")
class HomeworkByIdController {

    private final HomeworkApi homeworkApi;
    private final AuthApi authApi;

    @GetMapping("/{homeworkId}")
    @Operation(summary = "Get homework", description = "Get homework by id. Requires authentication.")
    public ResponseEntity<HomeworkDto> get(
            @PathVariable UUID homeworkId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        HomeworkDto dto = homeworkApi.get(homeworkId, requesterId)
                .orElseThrow(() -> HomeworkErrors.homeworkNotFound(homeworkId));
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{homeworkId}")
    @Operation(summary = "Update homework", description = "Update homework. Use clearFiles=true to remove all file links (files are not deleted). Requires TEACHER or ADMIN role.")
    public ResponseEntity<HomeworkDto> update(
            @PathVariable UUID homeworkId,
            @Valid @RequestBody UpdateHomeworkRequest body,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        boolean clearFiles = Boolean.TRUE.equals(body.clearFiles());
        HomeworkDto dto = homeworkApi.update(
                homeworkId,
                body.title(),
                body.description(),
                body.points(),
                clearFiles,
                body.storedFileIds(),
                requesterId
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{homeworkId}")
    @Operation(summary = "Delete homework", description = "Delete homework. Attached files are not deleted. Requires TEACHER or ADMIN role.")
    public ResponseEntity<Void> delete(
            @PathVariable UUID homeworkId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        homeworkApi.delete(homeworkId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
