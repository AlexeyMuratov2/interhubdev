package com.example.interhubdev.user.internal;

import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRolesContaining(Role role);

    List<User> findByStatus(UserStatus status);

    List<User> findByRolesContainingAndStatus(Role role, UserStatus status);
}
