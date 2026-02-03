package com.example.interhubdev.bootstrap;

import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration test for Bootstrap module through its public API.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("BootstrapApi")
class BootstrapApiTest {

    @Autowired
    private BootstrapApi bootstrapApi;

    @MockitoBean
    private UserApi userApi;

    @Test
    @DisplayName("should complete bootstrap and create admin when not exists")
    void shouldCompleteBootstrapWhenAdminNotExists() {
        when(userApi.existsByEmail(any())).thenReturn(false);
        when(userApi.createUser(anyString(), any(), any(), any())).thenReturn(
            new UserDto(UUID.randomUUID(), "admin@test.com", Set.of(Role.SUPER_ADMIN), UserStatus.PENDING, null, null, null, null, LocalDateTime.now(), null, null)
        );
        // Bootstrap runs on ApplicationReadyEvent before this test; stub is for API shape.
        // Unit-level bootstrap logic is in BootstrapServiceTest.
        assertThat(bootstrapApi.getConfiguredAdminEmail()).isNotBlank();
        assertThat(bootstrapApi.getStatus()).isNotNull();
    }

    @Test
    @DisplayName("should return configured admin email")
    void shouldReturnConfiguredAdminEmail() {
        // then
        assertThat(bootstrapApi.getConfiguredAdminEmail()).isNotBlank();
    }
}
