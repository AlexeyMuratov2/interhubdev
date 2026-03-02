package com.example.interhubdev.composition.internal.attendance;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.composition.StudentAttendanceHistoryDto;
import com.example.interhubdev.composition.StudentAttendanceHistoryQueryApi;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for student attendance history composition endpoint.
 */
@RestController
@RequestMapping("/api/composition")
@RequiredArgsConstructor
@Tag(name = "Composition – Student attendance", description = "Student attendance history in an offering")
class StudentAttendanceHistoryQueryController {

    private final StudentAttendanceHistoryQueryApi studentAttendanceHistoryQueryApi;
    private final AuthApi authApi;

    @GetMapping("/students/{studentId}/offerings/{offeringId}/attendance-history")
    @Operation(summary = "Get student attendance history", description = "All lessons for the offering with student attendance and absence notices per lesson; missed count and notices count")
    public ResponseEntity<StudentAttendanceHistoryDto> getStudentAttendanceHistory(
            @PathVariable UUID studentId,
            @PathVariable UUID offeringId,
            HttpServletRequest request
    ) {
        var requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return ResponseEntity.ok(studentAttendanceHistoryQueryApi.getStudentAttendanceHistory(studentId, offeringId, requesterId));
    }
}
