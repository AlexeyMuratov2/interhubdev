package com.example.interhubdev.composition.internal.student;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.composition.StudentSubjectInfoDto;
import com.example.interhubdev.composition.StudentSubjectsDto;
import com.example.interhubdev.composition.StudentSubjectsQueryApi;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for student subjects composition endpoints.
 */
@RestController
@RequestMapping("/api/composition")
@RequiredArgsConstructor
@Tag(name = "Composition – Student subjects", description = "Student dashboard subjects and subject detail")
class StudentSubjectsQueryController {

    private final StudentSubjectsQueryApi studentSubjectsQueryApi;
    private final AuthApi authApi;

    @GetMapping("/student/subjects")
    @Operation(summary = "Get student subjects", description = "All subjects for which the student has at least one lesson; optional semesterNo filter")
    public ResponseEntity<StudentSubjectsDto> getStudentSubjects(
            @RequestParam(required = false) Integer semesterNo,
            HttpServletRequest request
    ) {
        var requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return ResponseEntity.ok(studentSubjectsQueryApi.getStudentSubjects(requesterId, Optional.ofNullable(semesterNo)));
    }

    @GetMapping("/student/subjects/{offeringId}/info")
    @Operation(summary = "Get student subject info",
            description = "Full subject detail for a student: subject, curriculum, offering, schedule, teachers with profiles, student statistics (attendance, homework, points), and all course materials with file metadata. Only for student in the offering's group or admin.")
    public ResponseEntity<StudentSubjectInfoDto> getStudentSubjectInfo(
            @PathVariable UUID offeringId,
            @RequestParam(required = false) UUID semesterId,
            HttpServletRequest request
    ) {
        var requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return ResponseEntity.ok(studentSubjectsQueryApi.getStudentSubjectInfo(
                offeringId, requesterId, Optional.ofNullable(semesterId)));
    }
}
