package com.example.interhubdev.invitation.internal;

import com.example.interhubdev.invitation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    // ==================== Admin endpoints ====================

    @GetMapping
    @Operation(summary = "Get all invitations", description = "Returns list of all invitations (admin only)")
    public ResponseEntity<List<InvitationDto>> findAll() {
        return ResponseEntity.ok(invitationApi.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invitation by ID")
    public ResponseEntity<InvitationDto> findById(@PathVariable UUID id) {
        return invitationApi.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get invitations by status")
    public ResponseEntity<List<InvitationDto>> findByStatus(@PathVariable InvitationStatus status) {
        return ResponseEntity.ok(invitationApi.findByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create new invitation", description = "Creates user and sends invitation email")
    public ResponseEntity<InvitationDto> create(
            @Valid @RequestBody CreateInvitationRequest request,
            @RequestHeader("X-User-Id") UUID invitedBy  // TODO: Replace with authenticated user
    ) {
        InvitationDto invitation = invitationApi.create(request, invitedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "Resend invitation email")
    public ResponseEntity<Void> resend(@PathVariable UUID id) {
        invitationApi.resend(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel invitation")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        invitationApi.cancel(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Public endpoints ====================

    @GetMapping("/validate")
    @Operation(summary = "Validate invitation token", description = "Public endpoint for token validation")
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

    // ==================== Exception handlers ====================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", e.getMessage()));
    }

    record ErrorResponse(String code, String message) {}
}
