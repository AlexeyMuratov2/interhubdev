package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.absencenotice.StudentAbsenceNoticePage;
import com.example.interhubdev.absencenotice.SubmitAbsenceNoticeRequest;
import com.example.interhubdev.attendance.AttendanceApi;
import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance/notices")
@RequiredArgsConstructor
@Tag(name = "Attendance - Student Notices", description = "Student operations for absence notices")
class AttendanceStudentController {

    private final AttendanceApi attendanceApi;
    private final AuthApi authApi;
    private final StudentApi studentApi;

    private UUID requireCurrentUser(HttpServletRequest request) {
        return authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
    }

    private UUID requireCurrentStudentId(HttpServletRequest request) {
        UUID userId = requireCurrentUser(request);
        StudentDto student = studentApi.findByUserId(userId)
                .orElseThrow(() -> Errors.notFound("Student profile not found for current user"));
        return student.id();
    }

    @PostMapping
    @Operation(summary = "Create absence notice", description = "Create a new absence notice for one or more lesson sessions. Students can only create notices for sessions in their groups. Request body: lessonSessionIds (list), type, reasonText, fileIds.")
    public ResponseEntity<AbsenceNoticeDto> createNotice(
            @Valid @RequestBody SubmitAbsenceNoticeRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID studentId = requireCurrentStudentId(httpRequest);
        AbsenceNoticeDto notice = attendanceApi.createAbsenceNotice(request, studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(notice);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update absence notice", description = "Update an existing absence notice (only SUBMITTED; not after teacher has responded). Only the notice owner can update it.")
    public ResponseEntity<AbsenceNoticeDto> updateNotice(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitAbsenceNoticeRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID studentId = requireCurrentStudentId(httpRequest);
        AbsenceNoticeDto notice = attendanceApi.updateAbsenceNotice(id, request, studentId);
        return ResponseEntity.ok(notice);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel absence notice", description = "Cancel an active (SUBMITTED) absence notice. Only the notice owner can cancel it.")
    public ResponseEntity<AbsenceNoticeDto> cancelNotice(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        UUID studentId = requireCurrentStudentId(httpRequest);
        AbsenceNoticeDto notice = attendanceApi.cancelAbsenceNotice(id, studentId);
        return ResponseEntity.ok(notice);
    }

    @GetMapping("/mine")
    @Operation(summary = "Get my absence notices", description = "Get paginated list of own absence notices with lesson, offering, and slot context. Query: from, to (ISO datetime), cursor (notice ID for next page), limit (max 30). Only the authenticated student can access.")
    public ResponseEntity<StudentAbsenceNoticePage> getMyNotices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        UUID studentId = requireCurrentStudentId(httpRequest);
        StudentAbsenceNoticePage page = attendanceApi.getMyAbsenceNotices(studentId, from, to, cursor, limit);
        return ResponseEntity.ok(page);
    }
}
