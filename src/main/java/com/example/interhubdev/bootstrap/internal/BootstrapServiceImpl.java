package com.example.interhubdev.bootstrap.internal;

import com.example.interhubdev.bootstrap.BootstrapApi;
import com.example.interhubdev.bootstrap.BootstrapStatus;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of the Bootstrap API.
 * Handles creation of the initial SUPER_ADMIN and tracks bootstrap status.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminBootstrapProperties.class)
@Slf4j
class BootstrapServiceImpl implements BootstrapApi {

    private final UserApi userApi;
    private final AdminBootstrapProperties properties;

    private final AtomicReference<BootstrapStatus> status = new AtomicReference<>(BootstrapStatus.NOT_STARTED);
    private volatile boolean adminCreatedOnStartup = false;

    @Override
    public boolean isCompleted() {
        return status.get() == BootstrapStatus.COMPLETED;
    }

    @Override
    public BootstrapStatus getStatus() {
        return status.get();
    }

    @Override
    public String getConfiguredAdminEmail() {
        return properties.getEmail();
    }

    @Override
    public boolean wasAdminCreatedOnStartup() {
        return adminCreatedOnStartup;
    }

    /**
     * Execute the bootstrap process.
     * Creates SUPER_ADMIN if not exists.
     *
     * @return true if bootstrap completed successfully
     */
    @Transactional
    public boolean executeBootstrap() {
        String email = properties.getEmail();

        try {
            if (userApi.existsByEmail(email)) {
                log.info("Bootstrap SUPER_ADMIN already exists: {}", email);
                adminCreatedOnStartup = false;
            } else {
                log.info("Creating bootstrap SUPER_ADMIN: {}", email);
                createSuperAdmin(email);
                adminCreatedOnStartup = true;
                log.info("Bootstrap SUPER_ADMIN created successfully: {}", email);
            }

            status.set(BootstrapStatus.COMPLETED);
            return true;

        } catch (Exception e) {
            log.error("Bootstrap failed: {}", e.getMessage(), e);
            status.set(BootstrapStatus.FAILED);
            return false;
        }
    }

    private void createSuperAdmin(String email) {
        UserDto user = userApi.createUser(
                email,
                Set.of(Role.SUPER_ADMIN),
                properties.getFirstName(),
                properties.getLastName()
        );

        // activateUser handles password encoding internally
        userApi.activateUser(user.id(), properties.getPassword());
    }
}
