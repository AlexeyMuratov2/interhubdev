package com.example.interhubdev.bootstrap;

import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Bootstrap service logic.
 * Uses reflection to access internal classes for thorough testing.
 */
@DisplayName("BootstrapService")
class BootstrapServiceTest {

    private final UserApi userApi = mock(UserApi.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    @DisplayName("should create SUPER_ADMIN with correct attributes when not exists")
    void shouldCreateSuperAdminWhenNotExists() throws Exception {
        // given
        Object properties = createProperties("admin@test.com", "securePassword123", "Super", "Admin");
        BootstrapApi service = createService(properties);

        UserDto createdUser = new UserDto(
                UUID.randomUUID(),
                "admin@test.com",
                Role.SUPER_ADMIN,
                UserStatus.PENDING,
                "Super",
                "Admin",
                LocalDateTime.now(),
                null
        );

        when(userApi.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("$2a$encoded");
        when(userApi.createUser(anyString(), any(), anyString(), anyString())).thenReturn(createdUser);

        // when
        executeBootstrap(service);

        // then
        verify(userApi).createUser(
                eq("admin@test.com"),
                eq(Role.SUPER_ADMIN),
                eq("Super"),
                eq("Admin")
        );

        verify(userApi).activateUser(eq(createdUser.id()), eq("$2a$encoded"));

        assertThat(service.isCompleted()).isTrue();
        assertThat(service.getStatus()).isEqualTo(BootstrapStatus.COMPLETED);
        assertThat(service.wasAdminCreatedOnStartup()).isTrue();
    }

    @Test
    @DisplayName("should not create SUPER_ADMIN when already exists")
    void shouldNotCreateSuperAdminWhenExists() throws Exception {
        // given
        Object properties = createProperties("admin@test.com", "securePassword123", "Super", "Admin");
        BootstrapApi service = createService(properties);

        when(userApi.existsByEmail("admin@test.com")).thenReturn(true);

        // when
        executeBootstrap(service);

        // then
        verify(userApi, never()).createUser(anyString(), any(), anyString(), anyString());
        verify(userApi, never()).activateUser(any(), anyString());
        verify(passwordEncoder, never()).encode(anyString());

        assertThat(service.isCompleted()).isTrue();
        assertThat(service.wasAdminCreatedOnStartup()).isFalse();
    }

    @Test
    @DisplayName("should encode password with BCrypt")
    void shouldEncodePassword() throws Exception {
        // given
        Object properties = createProperties("admin@test.com", "securePassword123", null, null);
        BootstrapApi service = createService(properties);

        UserDto createdUser = new UserDto(
                UUID.randomUUID(),
                "admin@test.com",
                Role.SUPER_ADMIN,
                UserStatus.PENDING,
                null,
                null,
                LocalDateTime.now(),
                null
        );

        when(userApi.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("$2a$10$encodedHash");
        when(userApi.createUser(anyString(), any(), any(), any())).thenReturn(createdUser);

        // when
        executeBootstrap(service);

        // then
        verify(passwordEncoder).encode("securePassword123");
        verify(userApi).activateUser(eq(createdUser.id()), eq("$2a$10$encodedHash"));
    }

    @Test
    @DisplayName("should return configured admin email")
    void shouldReturnConfiguredEmail() throws Exception {
        // given
        Object properties = createProperties("admin@university.edu", "password123", null, null);
        BootstrapApi service = createService(properties);

        // then
        assertThat(service.getConfiguredAdminEmail()).isEqualTo("admin@university.edu");
    }

    @Test
    @DisplayName("should have NOT_STARTED status before bootstrap")
    void shouldHaveNotStartedStatusInitially() throws Exception {
        // given
        Object properties = createProperties("admin@test.com", "password123", null, null);
        BootstrapApi service = createService(properties);

        // then - before executeBootstrap
        assertThat(service.getStatus()).isEqualTo(BootstrapStatus.NOT_STARTED);
        assertThat(service.isCompleted()).isFalse();
    }

    // Helper methods to create internal classes via reflection

    private Object createProperties(String email, String password, String firstName, String lastName) throws Exception {
        Class<?> propsClass = Class.forName("com.example.interhubdev.bootstrap.internal.AdminBootstrapProperties");
        Object props = propsClass.getDeclaredConstructor().newInstance();

        setField(props, "email", email);
        setField(props, "password", password);
        setField(props, "firstName", firstName);
        setField(props, "lastName", lastName);

        return props;
    }

    private BootstrapApi createService(Object properties) throws Exception {
        Class<?> serviceClass = Class.forName("com.example.interhubdev.bootstrap.internal.BootstrapServiceImpl");
        Constructor<?> constructor = serviceClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return (BootstrapApi) constructor.newInstance(userApi, passwordEncoder, properties);
    }

    private void executeBootstrap(BootstrapApi service) throws Exception {
        service.getClass().getMethod("executeBootstrap").invoke(service);
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
