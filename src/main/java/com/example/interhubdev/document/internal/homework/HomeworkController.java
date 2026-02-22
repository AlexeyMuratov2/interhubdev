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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for homework: list by lesson, create.
 * Delegates to {@link HomeworkApi}.
 */
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Homework", description = "Homework assignments linked to lessons")
class HomeworkController {

    private final HomeworkApi homeworkApi;
    private final AuthApi authApi;

    @GetMapping("/{lessonId}/homework")
    @Operation(summary = "List homework", description = "List all homework for a lesson. Requires authentication.")
    public ResponseEntity<List<HomeworkDto>> listByLesson(
            @PathVariable UUID lessonId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        List<HomeworkDto> list = homeworkApi.listByLesson(lessonId, requesterId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{lessonId}/homework")
    @Operation(summary = "Create homework", description = "Create homework for a lesson. Requires TEACHER or ADMIN role.")
    public ResponseEntity<HomeworkDto> create(
            @PathVariable UUID lessonId,
            @Valid @RequestBody CreateHomeworkRequest body,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        HomeworkDto dto = homeworkApi.create(
                lessonId,
                body.title(),
                body.description(),
                body.points(),
                body.storedFileIds(),
                requesterId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
