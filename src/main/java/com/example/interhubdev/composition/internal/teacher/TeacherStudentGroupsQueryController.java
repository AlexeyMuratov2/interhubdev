package com.example.interhubdev.composition.internal.teacher;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.composition.TeacherStudentGroupsDto;
import com.example.interhubdev.composition.TeacherStudentGroupsQueryApi;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for teacher student groups composition endpoint.
 */
@RestController
@RequestMapping("/api/composition")
@RequiredArgsConstructor
@Tag(name = "Composition – Teacher", description = "Teacher dashboard student groups")
class TeacherStudentGroupsQueryController {

    private final TeacherStudentGroupsQueryApi teacherStudentGroupsQueryApi;
    private final AuthApi authApi;

    @GetMapping("/teacher/student-groups")
    @Operation(summary = "Get teacher student groups", description = "Groups where the teacher has at least one lesson (slots with lessons only)")
    public ResponseEntity<TeacherStudentGroupsDto> getTeacherStudentGroups(HttpServletRequest request) {
        var requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return ResponseEntity.ok(teacherStudentGroupsQueryApi.getTeacherStudentGroups(requesterId));
    }
}
