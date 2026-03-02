package com.example.interhubdev.composition.internal.group;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.composition.GroupSubjectInfoDto;
import com.example.interhubdev.composition.GroupSubjectQueryApi;
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
 * REST controller for group subject info composition endpoint.
 */
@RestController
@RequestMapping("/api/composition")
@RequiredArgsConstructor
@Tag(name = "Composition – Group subject", description = "Teacher group subject info")
class GroupSubjectQueryController {

    private final GroupSubjectQueryApi groupSubjectQueryApi;
    private final AuthApi authApi;

    @GetMapping("/groups/{groupId}/subjects/{subjectId}/info")
    @Operation(summary = "Get group subject info", description = "Full info for group and subject: offering, slots, curriculum, students with points, homework submissions, attendance. Only for teacher of this offering or admin.")
    public ResponseEntity<GroupSubjectInfoDto> getGroupSubjectInfo(
            @PathVariable UUID groupId,
            @PathVariable UUID subjectId,
            @RequestParam(required = false) UUID semesterId,
            HttpServletRequest request
    ) {
        var requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        return ResponseEntity.ok(groupSubjectQueryApi.getGroupSubjectInfo(
                groupId, subjectId, requesterId, Optional.ofNullable(semesterId)));
    }
}
