package com.example.interhubdev.account.internal;

import com.example.interhubdev.account.AccountApi;
import com.example.interhubdev.account.StudentListPage;
import com.example.interhubdev.account.StudentProfileItem;
import com.example.interhubdev.account.TeacherListPage;
import com.example.interhubdev.account.TeacherProfileItem;
import com.example.interhubdev.account.UpdateProfileRequest;
import com.example.interhubdev.account.UpdateUserRequest;
import com.example.interhubdev.account.UserWithProfilesDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.student.CreateStudentRequest;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.teacher.CreateTeacherRequest;
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.Role;
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
    @Operation(summary = "Get user by ID with all role profiles", description = "Returns user and optional teacher/student profiles. List endpoint /users is unchanged.")
    public ResponseEntity<UserWithProfilesDto> getUser(@PathVariable UUID id) {
        UserWithProfilesDto dto = accountApi.getUserWithProfiles(id)
                .orElseThrow(() -> AccountErrors.userNotFound(id));
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update user", description = "Update profile and/or roles. Email cannot be changed. Only SUPER_ADMIN can change teacherId/studentId.")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest body,
            HttpServletRequest request
    ) {
        UserDto current = accountApi.getCurrentUser(request)
                .orElseThrow(() -> Errors.unauthorized("Требуется вход в систему."));
        UserDto updated = accountApi.updateUser(id, body, current.id());
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

    // --------------- Teachers: list and get (mod/admin); edit only owner ---------------

    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List teachers (cursor pagination)", description = "Max 30 per page. Items include display name.")
    public ResponseEntity<TeacherListPage> listTeachers(
            @RequestParam(required = false) Optional<UUID> cursor,
            @RequestParam(required = false, defaultValue = "30") int limit
    ) {
        int size = limit <= 0 ? DEFAULT_PAGE_SIZE : Math.min(limit, MAX_PAGE_SIZE);
        return ResponseEntity.ok(accountApi.listTeachers(cursor.orElse(null), size));
    }

    @GetMapping("/teachers/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my teacher profile", description = "Returns current user's teacher profile. 404 if user is not a teacher.")
    public ResponseEntity<TeacherProfileItem> getMyTeacher(HttpServletRequest request) {
        UserDto current = accountApi.getCurrentUser(request)
                .orElseThrow(() -> Errors.unauthorized("Требуется вход в систему."));
        TeacherProfileItem item = accountApi.getTeacher(current.id())
                .orElseThrow(() -> AccountErrors.teacherProfileNotFound(current.id()));
        return ResponseEntity.ok(item);
    }

    @GetMapping("/teachers/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get teacher by user ID", description = "Use 'me' for current user, or a UUID. Mod/admin can use any UUID; others only 'me'.")
    public ResponseEntity<TeacherProfileItem> getTeacher(
            @PathVariable String userId,
            HttpServletRequest request
    ) {
        UUID resolved = resolveUserId(userId, request);
        TeacherProfileItem item = accountApi.getTeacher(resolved)
                .orElseThrow(() -> AccountErrors.teacherProfileNotFound(resolved));
        return ResponseEntity.ok(item);
    }

    @PatchMapping("/teachers/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update my teacher profile", description = "Only the account owner. Admin cannot edit teacher data.")
    public ResponseEntity<TeacherDto> updateMyTeacherProfile(
            HttpServletRequest request,
            @Valid @RequestBody CreateTeacherRequest body
    ) {
        UserDto current = accountApi.getCurrentUser(request)
                .orElseThrow(() -> Errors.unauthorized("Требуется вход в систему."));
        return ResponseEntity.ok(accountApi.updateMyTeacherProfile(current.id(), body));
    }

    // --------------- Students: list and get (mod/admin); edit only owner ---------------

    @GetMapping("/students/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my student profile", description = "Returns current user's student profile. 404 if user is not a student.")
    public ResponseEntity<StudentProfileItem> getMyStudent(HttpServletRequest request) {
        UserDto current = accountApi.getCurrentUser(request)
                .orElseThrow(() -> Errors.unauthorized("Требуется вход в систему."));
        StudentProfileItem item = accountApi.getStudent(current.id())
                .orElseThrow(() -> AccountErrors.studentProfileNotFound(current.id()));
        return ResponseEntity.ok(item);
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List students (cursor pagination)", description = "Max 30 per page. Items include display name.")
    public ResponseEntity<StudentListPage> listStudents(
            @RequestParam(required = false) Optional<UUID> cursor,
            @RequestParam(required = false, defaultValue = "30") int limit
    ) {
        int size = limit <= 0 ? DEFAULT_PAGE_SIZE : Math.min(limit, MAX_PAGE_SIZE);
        return ResponseEntity.ok(accountApi.listStudents(cursor.orElse(null), size));
    }

    @GetMapping("/students/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get student by user ID", description = "Use 'me' for current user, or a UUID. Mod/admin can use any UUID; others only 'me'.")
    public ResponseEntity<StudentProfileItem> getStudent(
            @PathVariable String userId,
            HttpServletRequest request
    ) {
        UUID resolved = resolveUserId(userId, request);
        StudentProfileItem item = accountApi.getStudent(resolved)
                .orElseThrow(() -> AccountErrors.studentProfileNotFound(resolved));
        return ResponseEntity.ok(item);
    }

    @PatchMapping("/students/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update my student profile", description = "Only the account owner. Admin cannot edit student data.")
    public ResponseEntity<StudentDto> updateMyStudentProfile(
            HttpServletRequest request,
            @Valid @RequestBody CreateStudentRequest body
    ) {
        UserDto current = accountApi.getCurrentUser(request)
                .orElseThrow(() -> Errors.unauthorized("Требуется вход в систему."));
        return ResponseEntity.ok(accountApi.updateMyStudentProfile(current.id(), body));
    }

    /**
     * Resolves path segment to user UUID: "me" (case-insensitive) → current user;
     * otherwise parses as UUID. Only MODERATOR/ADMIN/SUPER_ADMIN may use a UUID other than self.
     */
    private UUID resolveUserId(String userId, HttpServletRequest request) {
        if ("me".equalsIgnoreCase(userId)) {
            UserDto current = accountApi.getCurrentUser(request)
                    .orElseThrow(() -> Errors.unauthorized("Требуется вход в систему."));
            return current.id();
        }
        try {
            UUID id = UUID.fromString(userId);
            UserDto current = accountApi.getCurrentUser(request)
                    .orElseThrow(() -> Errors.unauthorized("Требуется вход в систему."));
            if (current.id().equals(id)
                    || current.hasRole(Role.MODERATOR) || current.hasRole(Role.ADMIN) || current.hasRole(Role.SUPER_ADMIN)) {
                return id;
            }
            throw Errors.forbidden("Доступ к профилю другого пользователя разрешён только модераторам и администраторам.");
        } catch (IllegalArgumentException e) {
            throw Errors.badRequest("Неверный идентификатор пользователя. Используйте 'me' или UUID.");
        }
    }
}
