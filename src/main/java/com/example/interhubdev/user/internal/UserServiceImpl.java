package com.example.interhubdev.user.internal;

import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserPage;
import com.example.interhubdev.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class UserServiceImpl implements UserApi {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Optional<UserDto> findById(UUID id) {
        return userRepository.findById(id).map(this::toDto);
    }

    @Override
    public List<UserDto> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(ids).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserDto> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toDto);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> findByRole(Role role) {
        return userRepository.findByRolesContaining(role).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> findByStatus(UserStatus status) {
        return userRepository.findByStatus(status).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDto createUser(String email, Collection<Role> roles, String firstName, String lastName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }
        Set<Role> roleSet = roles == null ? Set.of() : Set.copyOf(roles);
        if (roleSet.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
        Role.validateAtMostOneStaffType(roleSet);

        // Build user with empty roles, then add in place so Hibernate persists all rows (replacing collection can persist only one)
        User user = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .status(UserStatus.PENDING)
                .build();
        user.getRoles().addAll(roleSet);

        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void activateUser(UUID userId, String rawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("User is not in PENDING status");
        }

        user.activate(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void disableUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setStatus(UserStatus.DISABLED);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void enableUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getPasswordHash() == null) {
            throw new IllegalStateException("Cannot enable user without password. User must complete activation first.");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void reactivateForReinvite(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getStatus() != UserStatus.DISABLED) {
            throw new IllegalStateException("User must be DISABLED to reactivate for re-invite");
        }
        if (user.getPasswordHash() != null) {
            throw new IllegalStateException("User already activated; cannot reactivate for re-invite");
        }

        user.setStatus(UserStatus.PENDING);
        userRepository.save(user);
    }

    /**
     * Valid BCrypt hash used only for constant-time behaviour when user is not found.
     * Ensures response time does not reveal whether the email exists.
     */
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Override
    public boolean verifyPassword(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email).filter(User::canLogin);
        if (userOpt.isEmpty()) {
            passwordEncoder.matches(rawPassword, DUMMY_BCRYPT_HASH);
            return false;
        }
        return passwordEncoder.matches(rawPassword, userOpt.get().getPasswordHash());
    }

    @Override
    @Transactional
    public void updateLastLoginAt(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserDto updateProfile(UUID userId, String firstName, String lastName, String phone, LocalDate birthDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phone != null) user.setPhone(phone);
        if (birthDate != null) user.setBirthDate(birthDate);
        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto updateRoles(UUID userId, Set<Role> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Set<Role> roleSet = roles == null ? Set.of() : Set.copyOf(roles);
        if (roleSet.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
        Role.validateAtMostOneStaffType(roleSet);
        // Modify collection in place so Hibernate persists all rows (replacing with new Set can persist only one)
        user.getRoles().clear();
        user.getRoles().addAll(roleSet);
        return toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        userRepository.delete(user);
    }

    @Override
    public UserPage listUsers(UUID cursor, int limit) {
        int capped = Math.min(Math.max(1, limit), 30);
        List<User> slice = cursor == null
                ? userRepository.findFirst31ByOrderByIdAsc()
                : userRepository.findFirst31ByIdGreaterThanOrderByIdAsc(cursor);
        boolean hasMore = slice.size() > capped;
        List<User> pageUsers = hasMore ? slice.subList(0, capped) : slice;
        UUID nextCursor = hasMore ? pageUsers.get(pageUsers.size() - 1).getId() : null;
        return new UserPage(
                pageUsers.stream().map(this::toDto).collect(Collectors.toList()),
                nextCursor
        );
    }

    private UserDto toDto(User user) {
        // Load roles directly from user_roles table so all rows are returned (avoids ElementCollection returning only one)
        List<Role> roles = userRepository.findRoleNamesByUserId(user.getId()).stream()
                .map(Role::valueOf)
                .toList();
        return new UserDto(
                user.getId(),
                user.getEmail(),
                roles,
                user.getStatus(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getBirthDate(),
                user.getCreatedAt(),
                user.getActivatedAt(),
                user.getLastLoginAt()
        );
    }
}
