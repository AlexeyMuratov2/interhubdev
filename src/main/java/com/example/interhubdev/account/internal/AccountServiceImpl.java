package com.example.interhubdev.account.internal;

import com.example.interhubdev.account.AccountApi;
import com.example.interhubdev.account.StudentListPage;
import com.example.interhubdev.account.StudentProfileItem;
import com.example.interhubdev.account.TeacherListPage;
import com.example.interhubdev.account.TeacherProfileItem;
import com.example.interhubdev.account.UpdateProfileRequest;
import com.example.interhubdev.account.UpdateUserRequest;
import com.example.interhubdev.account.UserWithProfilesDto;
import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.invitation.InvitationApi;
import com.example.interhubdev.student.CreateStudentRequest;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.student.StudentPage;
import com.example.interhubdev.teacher.CreateTeacherRequest;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.teacher.TeacherPage;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserPage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Optional<UserWithProfilesDto> getUserWithProfiles(UUID id) {
        Optional<UserDto> userOpt = userApi.findById(id);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        UserDto user = userOpt.get();
        Optional<TeacherDto> teacher = teacherApi.findByUserId(id);
        Optional<StudentDto> student = studentApi.findByUserId(id);
        return Optional.of(UserWithProfilesDto.withProfiles(
                user,
                teacher.orElse(null),
                student.orElse(null)
        ));
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

    @Override
    public TeacherListPage listTeachers(UUID cursor, int limit) {
        int capped = Math.min(Math.max(1, limit), MAX_PAGE_SIZE);
        TeacherPage page = teacherApi.listTeachers(cursor, capped);
        if (page.items().isEmpty()) {
            return new TeacherListPage(List.of(), page.nextCursor());
        }
        List<UUID> userIds = page.items().stream().map(TeacherDto::userId).toList();
        List<UserDto> users = userApi.findByIds(userIds);
        var userMap = users.stream().collect(Collectors.toMap(UserDto::id, u -> u));
        List<TeacherProfileItem> items = page.items().stream()
                .map(t -> new TeacherProfileItem(t, teacherDisplayName(t, userMap.get(t.userId()))))
                .toList();
        return new TeacherListPage(items, page.nextCursor());
    }

    @Override
    public Optional<TeacherProfileItem> getTeacher(UUID userId) {
        return teacherApi.findByUserId(userId)
                .flatMap(teacher -> userApi.findById(userId)
                        .map(user -> new TeacherProfileItem(teacher, teacherDisplayName(teacher, user))));
    }

    @Override
    @Transactional
    public TeacherDto updateMyTeacherProfile(UUID currentUserId, CreateTeacherRequest request) {
        if (teacherApi.findByUserId(currentUserId).isEmpty()) {
            throw AccountErrors.teacherProfileNotFound(currentUserId);
        }
        return teacherApi.update(currentUserId, request);
    }

    @Override
    public StudentListPage listStudents(UUID cursor, int limit) {
        int capped = Math.min(Math.max(1, limit), MAX_PAGE_SIZE);
        StudentPage page = studentApi.listStudents(cursor, capped);
        if (page.items().isEmpty()) {
            return new StudentListPage(List.of(), page.nextCursor());
        }
        List<UUID> userIds = page.items().stream().map(StudentDto::userId).toList();
        List<UserDto> users = userApi.findByIds(userIds);
        var userMap = users.stream().collect(Collectors.toMap(UserDto::id, u -> u));
        List<StudentProfileItem> items = page.items().stream()
                .map(s -> new StudentProfileItem(s, studentDisplayName(s, userMap.get(s.userId()))))
                .toList();
        return new StudentListPage(items, page.nextCursor());
    }

    @Override
    public Optional<StudentProfileItem> getStudent(UUID userId) {
        return studentApi.findByUserId(userId)
                .flatMap(student -> userApi.findById(userId)
                        .map(user -> new StudentProfileItem(student, studentDisplayName(student, user))));
    }

    @Override
    @Transactional
    public StudentDto updateMyStudentProfile(UUID currentUserId, CreateStudentRequest request) {
        if (studentApi.findByUserId(currentUserId).isEmpty()) {
            throw AccountErrors.studentProfileNotFound(currentUserId);
        }
        return studentApi.update(currentUserId, request);
    }

    private static String teacherDisplayName(TeacherDto teacher, UserDto user) {
        if (teacher.englishName() != null && !teacher.englishName().isBlank()) {
            return teacher.englishName();
        }
        return user != null ? user.getFullName() : "";
    }

    private static String studentDisplayName(StudentDto student, UserDto user) {
        if (student.chineseName() != null && !student.chineseName().isBlank()) {
            return student.chineseName();
        }
        return user != null ? user.getFullName() : "";
    }
}
