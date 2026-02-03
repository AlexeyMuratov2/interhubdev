package com.example.interhubdev.account.internal;

import com.example.interhubdev.account.AccountApi;
import com.example.interhubdev.account.UpdateProfileRequest;
import com.example.interhubdev.account.UpdateUserRequest;
import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.invitation.InvitationApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserPage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of AccountApi.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AccountServiceImpl implements AccountApi {

    private static final int MAX_PAGE_SIZE = 30;

    private final UserApi userApi;
    private final AuthApi authApi;
    private final StudentApi studentApi;
    private final TeacherApi teacherApi;
    private final InvitationApi invitationApi;

    @Override
    public Optional<UserDto> getCurrentUser(HttpServletRequest request) {
        return authApi.getCurrentUser(request);
    }

    @Override
    public UserPage listUsers(UUID cursor, int limit) {
        return userApi.listUsers(cursor, Math.min(Math.max(1, limit), MAX_PAGE_SIZE));
    }

    @Override
    public Optional<UserDto> getUser(UUID id) {
        return userApi.findById(id);
    }

    @Override
    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        UserDto existing = userApi.findById(userId)
                .orElseThrow(() -> AccountErrors.userNotFound(userId));
        return userApi.updateProfile(
                userId,
                request != null ? request.firstName() : null,
                request != null ? request.lastName() : null,
                request != null ? request.phone() : null,
                request != null ? request.birthDate() : null
        );
    }

    @Override
    @Transactional
    public UserDto updateUser(UUID userId, UpdateUserRequest request) {
        userApi.findById(userId).orElseThrow(() -> AccountErrors.userNotFound(userId));
        if (request != null && (request.firstName() != null || request.lastName() != null
                || request.phone() != null || request.birthDate() != null)) {
            userApi.updateProfile(
                    userId,
                    request.firstName(),
                    request.lastName(),
                    request.phone(),
                    request.birthDate()
            );
        }
        if (request != null && request.roles() != null && !request.roles().isEmpty()) {
            userApi.updateRoles(userId, request.roles());
        }
        return userApi.findById(userId).orElseThrow(() -> AccountErrors.userNotFound(userId));
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId, UserDto currentUser) {
        if (userId.equals(currentUser.id())) {
            throw AccountErrors.cannotDeleteSelf();
        }
        UserDto target = userApi.findById(userId)
                .orElseThrow(() -> AccountErrors.userNotFound(userId));
        if (target.hasRole(Role.SUPER_ADMIN) && !currentUser.hasRole(Role.SUPER_ADMIN)) {
            throw AccountErrors.onlySuperAdminCanDeleteSuperAdmin();
        }
        authApi.revokeAllTokensForUser(userId);
        studentApi.findByUserId(userId).ifPresent(s -> studentApi.delete(userId));
        teacherApi.findByUserId(userId).ifPresent(t -> teacherApi.delete(userId));
        invitationApi.deleteByUserId(userId);
        userApi.deleteUser(userId);
    }
}
