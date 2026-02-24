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
    public UserDto updateUser(UUID userId, UpdateUserRequest request, UUID editorUserId) {
        if (request == null) {
            return userApi.findById(userId).orElseThrow(() -> AccountErrors.userNotFound(userId));
        }
        userApi.findById(userId).orElseThrow(() -> AccountErrors.userNotFound(userId));

        updateBasicProfileIfPresent(userId, request);
        UserDto updated = applyRolesAndCleanupProfiles(userId, request);
        applyStudentProfileIfPresent(userId, request, updated, editorUserId);
        applyTeacherProfileIfPresent(userId, request, updated, editorUserId);

        return userApi.findById(userId).orElseThrow(() -> AccountErrors.userNotFound(userId));
    }

    private void updateBasicProfileIfPresent(UUID userId, UpdateUserRequest request) {
        if (request.firstName() != null || request.lastName() != null
                || request.phone() != null || request.birthDate() != null) {
            userApi.updateProfile(
                    userId,
                    request.firstName(),
                    request.lastName(),
                    request.phone(),
                    request.birthDate()
            );
        }
    }

    private UserDto applyRolesAndCleanupProfiles(UUID userId, UpdateUserRequest request) {
        if (request.roles() == null || request.roles().isEmpty()) {
            return userApi.findById(userId).orElseThrow(() -> AccountErrors.userNotFound(userId));
        }
        userApi.updateRoles(userId, request.roles());
        UserDto updated = userApi.findById(userId).orElseThrow(() -> AccountErrors.userNotFound(userId));
        if (!updated.hasRole(Role.STUDENT)) {
            studentApi.findByUserId(userId).ifPresent(s -> studentApi.delete(userId));
        }
        if (!updated.hasRole(Role.TEACHER)) {
            teacherApi.findByUserId(userId).ifPresent(t -> teacherApi.delete(userId));
        }
        return updated;
    }

    private void applyStudentProfileIfPresent(UUID userId, UpdateUserRequest request, UserDto updated, UUID editorUserId) {
        if (request.studentProfile() == null) {
            return;
        }
        if (!updated.hasRole(Role.STUDENT)) {
            throw AccountErrors.studentProfileRequiresRole();
        }
        if (studentApi.findByUserId(userId).isEmpty()) {
            var missing = requireStudentProfileFieldsForCreate(request.studentProfile());
            if (!missing.isEmpty()) {
                throw AccountErrors.studentProfileCreateRequiredFields(missing);
            }
            studentApi.create(userId, request.studentProfile());
        } else {
            StudentDto existing = studentApi.findByUserId(userId).orElseThrow();
            String newStudentId = request.studentProfile().studentId();
            if (newStudentId != null && !newStudentId.equals(existing.studentId())) {
                UserDto editor = userApi.findById(editorUserId).orElseThrow(() -> AccountErrors.userNotFound(editorUserId));
                if (!editor.hasRole(Role.SUPER_ADMIN)) {
                    throw AccountErrors.onlySuperAdminCanChangeProfileId();
                }
            }
            studentApi.update(userId, request.studentProfile());
        }
    }

    private void applyTeacherProfileIfPresent(UUID userId, UpdateUserRequest request, UserDto updated, UUID editorUserId) {
        if (request.teacherProfile() == null) {
            return;
        }
        if (!updated.hasRole(Role.TEACHER)) {
            throw AccountErrors.teacherProfileRequiresRole();
        }
        if (teacherApi.findByUserId(userId).isEmpty()) {
            var missing = requireTeacherProfileFieldsForCreate(request.teacherProfile());
            if (!missing.isEmpty()) {
                throw AccountErrors.teacherProfileCreateRequiredFields(missing);
            }
            teacherApi.create(userId, request.teacherProfile());
        } else {
            TeacherDto existing = teacherApi.findByUserId(userId).orElseThrow();
            String newTeacherId = request.teacherProfile().teacherId();
            if (newTeacherId != null && !newTeacherId.equals(existing.teacherId())) {
                UserDto editor = userApi.findById(editorUserId).orElseThrow(() -> AccountErrors.userNotFound(editorUserId));
                if (!editor.hasRole(Role.SUPER_ADMIN)) {
                    throw AccountErrors.onlySuperAdminCanChangeProfileId();
                }
            }
            teacherApi.update(userId, request.teacherProfile());
        }
    }

    private static String requireStudentProfileFieldsForCreate(CreateStudentRequest profile) {
        var missing = new java.util.ArrayList<String>();
        if (profile.studentId() == null || profile.studentId().isBlank()) missing.add("studentId");
        if (profile.faculty() == null || profile.faculty().isBlank()) missing.add("faculty");
        return String.join(", ", missing);
    }

    private static String requireTeacherProfileFieldsForCreate(CreateTeacherRequest profile) {
        var missing = new java.util.ArrayList<String>();
        if (profile.teacherId() == null || profile.teacherId().isBlank()) missing.add("teacherId");
        if (profile.faculty() == null || profile.faculty().isBlank()) missing.add("faculty");
        return String.join(", ", missing);
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
        invitationApi.clearInvitedByForUser(userId);
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
        TeacherDto existing = teacherApi.findByUserId(currentUserId)
                .orElseThrow(() -> AccountErrors.teacherProfileNotFound(currentUserId));
        String newTeacherId = request != null ? request.teacherId() : null;
        if (newTeacherId != null && !newTeacherId.equals(existing.teacherId())) {
            UserDto editor = userApi.findById(currentUserId).orElseThrow(() -> AccountErrors.userNotFound(currentUserId));
            if (!editor.hasRole(Role.SUPER_ADMIN)) {
                throw AccountErrors.onlySuperAdminCanChangeProfileId();
            }
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
        StudentDto existing = studentApi.findByUserId(currentUserId)
                .orElseThrow(() -> AccountErrors.studentProfileNotFound(currentUserId));
        String newStudentId = request != null ? request.studentId() : null;
        if (newStudentId != null && !newStudentId.equals(existing.studentId())) {
            UserDto editor = userApi.findById(currentUserId).orElseThrow(() -> AccountErrors.userNotFound(currentUserId));
            if (!editor.hasRole(Role.SUPER_ADMIN)) {
                throw AccountErrors.onlySuperAdminCanChangeProfileId();
            }
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
