package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.*;
import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Attendance API: mark and query attendance records.
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Official attendance records marked by teachers for lesson sessions.")
class AttendanceController {

    private final AttendanceApi attendanceApi;
    private final AuthApi authApi;

    private UUID requireCurrentUser(HttpServletRequest request) {
        return authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
    }

    @PostMapping("/sessions/{sessionId}/records/bulk")
    @Operation(summary = "Bulk mark attendance for session", description = "Mark attendance for multiple students in a lesson session. All-or-nothing transaction. Requires TEACHER (of session) or ADMIN role.")
    public ResponseEntity<List<AttendanceRecordDto>> markAttendanceBulk(
            @PathVariable UUID sessionId,
            @Valid @RequestBody BulkMarkAttendanceRequest body,
            HttpServletRequest request
    ) {
        UUID markedBy = requireCurrentUser(request);
        List<AttendanceRecordDto> records = attendanceApi.markAttendanceBulk(sessionId, body.items(), markedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(records);
    }

    @PutMapping("/sessions/{sessionId}/students/{studentId}")
    @Operation(summary = "Mark attendance for single student", description = "Mark or update attendance for one student in a lesson session. Requires TEACHER (of session) or ADMIN role.")
    public ResponseEntity<AttendanceRecordDto> markAttendanceSingle(
            @PathVariable UUID sessionId,
            @PathVariable UUID studentId,
            @Valid @RequestBody MarkAttendanceRequest body,
            HttpServletRequest request
    ) {
        UUID markedBy = requireCurrentUser(request);
        AttendanceRecordDto record = attendanceApi.markAttendanceSingle(
                sessionId,
                studentId,
                body.status(),
                body.minutesLate(),
                body.teacherComment(),
                markedBy
        );
        return ResponseEntity.ok(record);
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get session attendance", description = "Get attendance records for a lesson session with roster and counts. Requires TEACHER (of session) or ADMIN role.")
    public ResponseEntity<SessionAttendanceDto> getSessionAttendance(
            @PathVariable UUID sessionId,
            HttpServletRequest request
    ) {
        UUID requesterId = requireCurrentUser(request);
        SessionAttendanceDto dto = attendanceApi.getSessionAttendance(sessionId, requesterId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/students/{studentId}")
    @Operation(summary = "Get student attendance history", description = "Get attendance history for a student with summary. Query: from, to (ISO datetime), offeringId, groupId. Students can only view own records.")
    public ResponseEntity<StudentAttendanceDto> getStudentAttendance(
            @PathVariable UUID studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) UUID offeringId,
            @RequestParam(required = false) UUID groupId,
            HttpServletRequest request
    ) {
        UUID requesterId = requireCurrentUser(request);
        StudentAttendanceDto dto = attendanceApi.getStudentAttendance(
                studentId, from, to, offeringId, groupId, requesterId
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/groups/{groupId}/summary")
    @Operation(summary = "Get group attendance summary", description = "Get attendance summary for a group (per-student counts). Query: from, to (ISO date), offeringId. Requires TEACHER (of group) or ADMIN role.")
    public ResponseEntity<GroupAttendanceSummaryDto> getGroupAttendanceSummary(
            @PathVariable UUID groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID offeringId,
            HttpServletRequest request
    ) {
        UUID requesterId = requireCurrentUser(request);
        GroupAttendanceSummaryDto dto = attendanceApi.getGroupAttendanceSummary(
                groupId, from, to, offeringId, requesterId
        );
        return ResponseEntity.ok(dto);
    }

    /**
     * Request for bulk attendance marking.
     */
    public record BulkMarkAttendanceRequest(
            @Valid List<@Valid MarkAttendanceItem> items
    ) {
    }

    /**
     * Request for single attendance marking.
     */
    public record MarkAttendanceRequest(
            @jakarta.validation.constraints.NotNull(message = "status is required")
            AttendanceStatus status,
            Integer minutesLate,
            @jakarta.validation.constraints.Size(max = 2000, message = "teacherComment must not exceed 2000 characters")
            String teacherComment
    ) {
    }
}
