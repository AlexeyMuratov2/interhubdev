package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.absencenotice.AbsenceNoticeStatus;
import com.example.interhubdev.absencenotice.RespondToAbsenceNoticeRequest;
import com.example.interhubdev.absencenotice.TeacherAbsenceNoticePage;
import com.example.interhubdev.attendance.AttendanceApi;
import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance - Teacher Notices", description = "Teacher operations for viewing absence notices")
class AttendanceTeacherController {

    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 30;

    private final AttendanceApi attendanceApi;
    private final AuthApi authApi;

    private UUID requireCurrentUser(HttpServletRequest request) {
        return authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
    }

    @GetMapping("/sessions/{sessionId}/notices")
    @Operation(summary = "Get session absence notices", description = "Get list of absence notices for a lesson session. Query: includeCanceled (default: false). Only teachers of the session or admins can access.")
    public ResponseEntity<List<AbsenceNoticeDto>> getSessionNotices(
            @PathVariable UUID sessionId,
            @RequestParam(required = false, defaultValue = "false") boolean includeCanceled,
            HttpServletRequest httpRequest
    ) {
        UUID requesterId = requireCurrentUser(httpRequest);
        List<AbsenceNoticeDto> notices = attendanceApi.getSessionNotices(sessionId, requesterId, includeCanceled);
        return ResponseEntity.ok(notices);
    }

    @GetMapping("/teachers/me/notices")
    @Operation(summary = "Get all absence notices for current teacher", description = "Get all absence notices for all lesson sessions where the current teacher is assigned to the offering. Each item includes notice, student, lesson, offering, and group context. Supports status filtering and cursor-based pagination. Max 30 per page. Only teachers can access.")
    public ResponseEntity<TeacherAbsenceNoticePage> getTeacherNotices(
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false, defaultValue = "30") int limit,
            HttpServletRequest httpRequest
    ) {
        UUID requesterId = requireCurrentUser(httpRequest);

        List<AbsenceNoticeStatus> statusList = null;
        if (statuses != null && !statuses.isBlank()) {
            try {
                statusList = Arrays.stream(statuses.split(","))
                        .map(String::trim)
                        .map(AbsenceNoticeStatus::valueOf)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw Errors.badRequest("Invalid status: " + statuses + ". Valid values: " +
                        Arrays.toString(AbsenceNoticeStatus.values()));
            }
        }

        int cappedLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

        TeacherAbsenceNoticePage page = attendanceApi.getTeacherAbsenceNotices(
                requesterId,
                statusList,
                cursor,
                cappedLimit
        );

        return ResponseEntity.ok(page);
    }

    @PostMapping("/notices/{id}/approve")
    @Operation(summary = "Approve absence notice", description = "Approve an absence notice. Only teachers of the session can approve. Optional comment in body.")
    public ResponseEntity<AbsenceNoticeDto> approveNotice(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) RespondToAbsenceNoticeRequest body,
            HttpServletRequest httpRequest
    ) {
        UUID requesterId = requireCurrentUser(httpRequest);
        String comment = body != null ? body.comment() : null;
        AbsenceNoticeDto notice = attendanceApi.respondToAbsenceNotice(id, true, comment, requesterId);
        return ResponseEntity.ok(notice);
    }

    @PostMapping("/notices/{id}/reject")
    @Operation(summary = "Reject absence notice", description = "Reject an absence notice. Only teachers of the session can reject. Optional comment in body.")
    public ResponseEntity<AbsenceNoticeDto> rejectNotice(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) RespondToAbsenceNoticeRequest body,
            HttpServletRequest httpRequest
    ) {
        UUID requesterId = requireCurrentUser(httpRequest);
        String comment = body != null ? body.comment() : null;
        AbsenceNoticeDto notice = attendanceApi.respondToAbsenceNotice(id, false, comment, requesterId);
        return ResponseEntity.ok(notice);
    }
}
