package com.example.interhubdev.invitation.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.invitation.*;
import com.example.interhubdev.user.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for invitation operations.
 */
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@Tag(name = "Invitations", description = "User invitation management")
class InvitationController {

    private final InvitationApi invitationApi;
    private final AuthApi authApi;

    // ==================== Staff/Moderator/Admin endpoints ====================

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 30;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(
            summary = "Get invitations (cursor pagination)",
            description = "Returns up to 30 invitations per request, sorted by send time (createdAt) descending. Use cursor for next page. STAFF, MODERATOR, ADMIN, SUPER_ADMIN."
    )
    public ResponseEntity<InvitationPage> findPage(
            @RequestParam(required = false) Optional<UUID> cursor,
            @RequestParam(required = false, defaultValue = "30") int limit
    ) {
        int size = limit <= 0 ? DEFAULT_PAGE_SIZE : Math.min(limit, MAX_PAGE_SIZE);
        InvitationPage page = invitationApi.findPage(cursor.orElse(null), size);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get invitation by ID")
    public ResponseEntity<InvitationDto> findById(@PathVariable UUID id) {
        InvitationDto dto = invitationApi.findById(id)
                .orElseThrow(() -> InvitationErrors.invitationNotFound(id));
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('STAFF', 'MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get invitations by status")
    public ResponseEntity<List<InvitationDto>> findByStatus(@PathVariable InvitationStatus status) {
        return ResponseEntity.ok(invitationApi.findByStatus(status));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create new invitation", description = "Only ADMIN and SUPER_ADMIN can create invitations")
    public ResponseEntity<InvitationDto> create(
            @Valid @RequestBody CreateInvitationRequest request,
            HttpServletRequest httpRequest
    ) {
        UserDto currentUser = authApi.getCurrentUser(httpRequest)
                .orElseThrow(() -> Errors.unauthorized("Для создания приглашения необходимо войти в систему."));
        InvitationDto invitation = invitationApi.create(request, currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Resend invitation email", description = "Only ADMIN and SUPER_ADMIN can resend invitations")
    public ResponseEntity<Void> resend(@PathVariable UUID id) {
        invitationApi.resend(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Cancel invitation", description = "Only ADMIN and SUPER_ADMIN can cancel invitations")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        invitationApi.cancel(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Public endpoints ====================

    @GetMapping("/validate")
    @Operation(
            summary = "Validate invitation token",
            description = """
                    Public endpoint. Returns validation result with field `code` for frontend branching.
                    - 200, valid=true: token valid, show activation form.
                    - 200, code=INVITATION_TOKEN_EXPIRED_EMAIL_RESENT: token expired, new email sent; show 'check your email'.
                    - 400, code=INVITATION_TOKEN_INVALID: token not found or already used.
                    - 400, code=INVITATION_EXPIRED: invitation period (90 days) expired.
                    - 400, code=INVITATION_NOT_ACCEPTABLE: invitation already accepted/cancelled.
                    See docs/invitation-api-responses.md for full response structure.
                    """
    )
    public ResponseEntity<TokenValidationResult> validateToken(@RequestParam String token) {
        TokenValidationResult result = invitationApi.validateToken(token);
        if (result.valid()) {
            return ResponseEntity.ok(result);
        } else if (result.tokenRegenerated()) {
            // Token expired, new email sent
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/accept")
    @Operation(summary = "Accept invitation", description = "Public endpoint to accept invitation and set password")
    public ResponseEntity<Void> accept(@Valid @RequestBody AcceptInvitationRequest request) {
        invitationApi.accept(request);
        return ResponseEntity.ok().build();
    }

}
