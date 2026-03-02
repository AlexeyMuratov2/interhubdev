package com.example.interhubdev.composition.internal.grades;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.composition.StudentGradeHistoryDto;
import com.example.interhubdev.composition.StudentGradeHistoryQueryApi;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for student grade history composition endpoint.
 */
@RestController
@RequestMapping("/api/composition")
@RequiredArgsConstructor
@Tag(name = "Composition – Student grades", description = "Student grade history in an offering")
class StudentGradeHistoryQueryController {

    private final StudentGradeHistoryQueryApi studentGradeHistoryQueryApi;
    private final AuthApi authApi;

    @GetMapping("/students/{studentId}/offerings/{offeringId}/grade-history")
    @Operation(summary = "Get student grade history", description = "All grades for a student in an offering with lesson, homework, submission and grader context")
    public ResponseEntity<StudentGradeHistoryDto> getStudentGradeHistory(
            @PathVariable UUID studentId,
            @PathVariable UUID offeringId,
            HttpServletRequest request
    ) {
        var requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return ResponseEntity.ok(studentGradeHistoryQueryApi.getStudentGradeHistory(studentId, offeringId, requesterId));
    }
}
