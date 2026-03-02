package com.example.interhubdev.composition.internal.attendance;

import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.composition.SessionAttendanceQueryApi;
import com.example.interhubdev.composition.SessionAttendanceViewDto;
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
 * REST controller for session attendance composition endpoints (records + notices merged).
 */
@RestController
@RequestMapping("/api/composition")
@RequiredArgsConstructor
@Tag(name = "Composition – Session attendance", description = "Session attendance and notices (merged view)")
class SessionAttendanceQueryController {

    private final SessionAttendanceQueryApi sessionAttendanceQueryApi;
    private final AuthApi authApi;

    @GetMapping("/sessions/{sessionId}/attendance")
    @Operation(summary = "Get session attendance", description = "Attendance records for a lesson session with roster and counts; includes absence notices per student. Query: includeCanceled (default: false). Requires TEACHER (of session) or ADMIN role.")
    public ResponseEntity<SessionAttendanceViewDto> getSessionAttendance(
            @PathVariable UUID sessionId,
            @RequestParam(required = false, defaultValue = "false") boolean includeCanceled,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
        return ResponseEntity.ok(sessionAttendanceQueryApi.getSessionAttendance(sessionId, requesterId, includeCanceled));
    }

    @GetMapping("/sessions/{sessionId}/notices")
    @Operation(summary = "Get session absence notices", description = "List of absence notices for a lesson session. Query: includeCanceled (default: false). Only teachers of the session or admins can access.")
    public ResponseEntity<List<AbsenceNoticeDto>> getSessionNotices(
            @PathVariable UUID sessionId,
            @RequestParam(required = false, defaultValue = "false") boolean includeCanceled,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
        List<AbsenceNoticeDto> notices = sessionAttendanceQueryApi.getSessionNotices(sessionId, requesterId, includeCanceled);
        return ResponseEntity.ok(notices);
    }
}
