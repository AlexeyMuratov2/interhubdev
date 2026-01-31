package com.example.interhubdev.user.internal;

import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.User;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class UserServiceImpl implements UserApi {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Override
    public List<User> findByStatus(UserStatus status) {
        return userRepository.findByStatus(status);
    }

    @Override
    @Transactional
    public User createUser(String email, Role role, String firstName, String lastName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }

        User user = User.builder()
                .email(email)
                .role(role)
                .firstName(firstName)
                .lastName(lastName)
                .status(UserStatus.PENDING)
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateUser(UUID userId, String encodedPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("User is not in PENDING status");
        }

        user.activate(encodedPassword);
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
    public User save(User user) {
        return userRepository.save(user);
    }
}
