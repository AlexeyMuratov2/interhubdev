package com.example.interhubdev.user.internal;

import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Load all role names for a user directly from user_roles table.
     * Use this when building DTOs to avoid relying on the entity's collection (which may not reflect all rows in some setups).
     */
    @Query(value = "SELECT role FROM user_roles WHERE user_id = :userId", nativeQuery = true)
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRolesContaining(Role role);

    List<User> findByStatus(UserStatus status);

    List<User> findByRolesContainingAndStatus(Role role, UserStatus status);

    List<User> findFirst31ByOrderByIdAsc();

    List<User> findFirst31ByIdGreaterThanOrderByIdAsc(UUID after);
}
