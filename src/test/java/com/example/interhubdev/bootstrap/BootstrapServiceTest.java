package com.example.interhubdev.bootstrap;

import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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

    @Test
    @DisplayName("should create SUPER_ADMIN with correct attributes when not exists")
    void shouldCreateSuperAdminWhenNotExists() throws Exception {
        // given
        Object properties = createProperties("admin@test.com", "securePassword123", "Super", "Admin");
        BootstrapApi service = createService(properties);

        UserDto createdUser = new UserDto(
                UUID.randomUUID(),
                "admin@test.com",
                List.of(Role.SUPER_ADMIN),
                UserStatus.PENDING,
                "Super",
                "Admin",
                null,
                null,
                LocalDateTime.now(),
                null,
                null
        );

        when(userApi.existsByEmail("admin@test.com")).thenReturn(false);
        when(userApi.createUser(anyString(), any(), anyString(), anyString())).thenReturn(createdUser);

        // when
        executeBootstrap(service);

        // then (createUser accepts Collection<Role>; impl may pass Set or List)
        verify(userApi).createUser(
                eq("admin@test.com"),
                eq(Set.of(Role.SUPER_ADMIN)),
                eq("Super"),
                eq("Admin")
        );

        verify(userApi).activateUser(eq(createdUser.id()), eq("securePassword123"));

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

        assertThat(service.isCompleted()).isTrue();
        assertThat(service.wasAdminCreatedOnStartup()).isFalse();
    }

    @Test
    @DisplayName("should pass raw password to activateUser (encoding is internal to UserApi)")
    void shouldPassRawPasswordToActivateUser() throws Exception {
        // given
        Object properties = createProperties("admin@test.com", "securePassword123", null, null);
        BootstrapApi service = createService(properties);

        UserDto createdUser = new UserDto(
                UUID.randomUUID(),
                "admin@test.com",
                List.of(Role.SUPER_ADMIN),
                UserStatus.PENDING,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                null,
                null
        );

        when(userApi.existsByEmail("admin@test.com")).thenReturn(false);
        when(userApi.createUser(anyString(), any(java.util.Collection.class), any(), any())).thenReturn(createdUser);

        // when
        executeBootstrap(service);

        // then - bootstrap passes raw password; UserApi encodes internally
        verify(userApi).activateUser(eq(createdUser.id()), eq("securePassword123"));
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
        var constructor = propsClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object props = constructor.newInstance();

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
        return (BootstrapApi) constructor.newInstance(userApi, properties);
    }

    private void executeBootstrap(BootstrapApi service) throws Exception {
        var method = service.getClass().getMethod("executeBootstrap");
        method.setAccessible(true);
        method.invoke(service);
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
