package com.example.interhubdev.composition.internal.homework;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.composition.StudentHomeworkHistoryDto;
import com.example.interhubdev.composition.StudentHomeworkHistoryQueryApi;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for student homework history composition endpoint.
 */
@RestController
@RequestMapping("/api/composition")
@RequiredArgsConstructor
@Tag(name = "Composition – Student homework", description = "Student homework history in an offering")
class StudentHomeworkHistoryQueryController {

    private final StudentHomeworkHistoryQueryApi studentHomeworkHistoryQueryApi;
    private final AuthApi authApi;

    @GetMapping("/students/{studentId}/offerings/{offeringId}/homework-history")
    @Operation(summary = "Get student homework history", description = "All homeworks for the offering with student's submission and grade per assignment; full data for frontend student view")
    public ResponseEntity<StudentHomeworkHistoryDto> getStudentHomeworkHistory(
            @PathVariable UUID studentId,
            @PathVariable UUID offeringId,
            HttpServletRequest request
    ) {
        var requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return ResponseEntity.ok(studentHomeworkHistoryQueryApi.getStudentHomeworkHistory(studentId, offeringId, requesterId));
    }
}
