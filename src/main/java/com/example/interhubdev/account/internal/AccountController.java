package com.example.interhubdev.account.internal;

import com.example.interhubdev.account.AccountApi;
import com.example.interhubdev.account.UpdateProfileRequest;
import com.example.interhubdev.account.UpdateUserRequest;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for account management (own profile and user management).
 */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Account and user management")
class AccountController {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 30;

    private final AccountApi accountApi;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user", description = "Returns the authenticated user's profile")
    public ResponseEntity<UserDto> getMe(HttpServletRequest request) {
        UserDto user = accountApi.getCurrentUser(request)
                .orElseThrow(() -> Errors.unauthorized("Требуется вход в систему."));
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update own profile", description = "Update current user profile (email cannot be changed)")
    public ResponseEntity<UserDto> updateMe(
            HttpServletRequest request,
            @Valid @RequestBody UpdateProfileRequest body
    ) {
        UserDto current = accountApi.getCurrentUser(request)
                .orElseThrow(() -> Errors.unauthorized("Требуется вход в систему."));
        UserDto updated = accountApi.updateProfile(current.id(), body);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List users (cursor pagination)", description = "Max 30 per page. MODERATOR, ADMIN, SUPER_ADMIN.")
    public ResponseEntity<UserPage> listUsers(
            @RequestParam(required = false) Optional<UUID> cursor,
            @RequestParam(required = false, defaultValue = "30") int limit
    ) {
        int size = limit <= 0 ? DEFAULT_PAGE_SIZE : Math.min(limit, MAX_PAGE_SIZE);
        UserPage page = accountApi.listUsers(cursor.orElse(null), size);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDto> getUser(@PathVariable UUID id) {
        UserDto user = accountApi.getUser(id)
                .orElseThrow(() -> AccountErrors.userNotFound(id));
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update user", description = "Update profile and/or roles. Email cannot be changed.")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest body
    ) {
        UserDto updated = accountApi.updateUser(id, body);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete user", description = "Only ADMIN and SUPER_ADMIN. Cannot delete self. SUPER_ADMIN only deletable by another SUPER_ADMIN.")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        UserDto current = accountApi.getCurrentUser(request)
                .orElseThrow(() -> Errors.unauthorized("Требуется вход в систему."));
        accountApi.deleteUser(id, current);
        return ResponseEntity.noContent().build();
    }
}
