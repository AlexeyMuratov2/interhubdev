package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeDto;
import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for teacher absence notice operations.
 * Teachers can view absence notices for sessions they teach.
 */
@RestController
@RequestMapping("/api/attendance/sessions/{sessionId}/notices")
@RequiredArgsConstructor
@Tag(name = "Attendance - Teacher Notices", description = "Teacher operations for viewing absence notices")
class AttendanceTeacherController {

    private final GetSessionAbsenceNoticesUseCase getSessionNoticesUseCase;
    private final AuthApi authApi;

    private UUID requireCurrentUser(HttpServletRequest request) {
        return authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
    }

    @GetMapping
    @Operation(summary = "Get session absence notices", description = "Get list of absence notices for a lesson session. Query: includeCanceled (default: false). Only teachers of the session or admins can access.")
    public ResponseEntity<List<AbsenceNoticeDto>> getSessionNotices(
            @PathVariable UUID sessionId,
            @RequestParam(required = false, defaultValue = "false") boolean includeCanceled,
            HttpServletRequest httpRequest
    ) {
        UUID requesterId = requireCurrentUser(httpRequest);
        List<AbsenceNoticeDto> notices = getSessionNoticesUseCase.execute(sessionId, requesterId, includeCanceled);
        return ResponseEntity.ok(notices);
    }
}
