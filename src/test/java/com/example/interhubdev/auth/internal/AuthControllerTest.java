package com.example.interhubdev.auth.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.auth.AuthApi.AuthenticationException;
import com.example.interhubdev.auth.AuthApi.AuthErrorCode;
import com.example.interhubdev.auth.AuthResult;
import com.example.interhubdev.auth.LoginRequest;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for AuthController.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AuthController")
class AuthControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "user@test.com";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthApi authApi;

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 and AuthResult when login successful")
        void success() throws Exception {
            AuthResult result = AuthResult.success(USER_ID, EMAIL, List.of(Role.STUDENT), "John Doe");
            when(authApi.login(eq(EMAIL), eq("password123"), any(), any())).thenReturn(result);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "password123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.roles[0]").value("STUDENT"))
                    .andExpect(jsonPath("$.fullName").value("John Doe"))
                    .andExpect(jsonPath("$.message").value("Login successful"));

            verify(authApi).login(eq(EMAIL), eq("password123"), any(), any());
        }

        @Test
        @DisplayName("returns 401 when invalid credentials")
        void invalidCredentials() throws Exception {
            when(authApi.login(anyString(), anyString(), any(), any()))
                    .thenThrow(new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "wrong"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("returns 403 when user not active")
        void userNotActive() throws Exception {
            when(authApi.login(anyString(), anyString(), any(), any()))
                    .thenThrow(new AuthenticationException(AuthErrorCode.USER_NOT_ACTIVE, "Account not activated."));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "password123"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("USER_NOT_ACTIVE"));
        }

        @Test
        @DisplayName("returns 400 when request invalid (empty email)")
        void validationError() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("", "password123"))))
                    .andExpect(status().isBadRequest());

            verify(authApi, org.mockito.Mockito.never()).login(anyString(), anyString(), any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("returns 200 and AuthResult when refresh successful")
        void success() throws Exception {
            AuthResult result = AuthResult.success(USER_ID, EMAIL, List.of(Role.STUDENT), "John Doe");
            when(authApi.refresh(any(), any())).thenReturn(result);

            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.roles[0]").value("STUDENT"));

            verify(authApi).refresh(any(), any());
        }

        @Test
        @DisplayName("returns 401 when refresh token invalid")
        void invalidToken() throws Exception {
            when(authApi.refresh(any(), any()))
                    .thenThrow(new AuthenticationException(AuthErrorCode.TOKEN_INVALID, "Invalid refresh token"));

            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("returns 204 when logout successful")
        void success() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isNoContent());

            verify(authApi).logout(any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetCurrentUser {

        @Test
        @DisplayName("returns 200 and user when authenticated")
        void authenticated() throws Exception {
            UserDto user = new UserDto(
                    USER_ID, EMAIL, Set.of(Role.STUDENT), UserStatus.ACTIVE,
                    "John", "Doe", null, null,
                    LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
            );
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(user));

            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.roles[0]").value("STUDENT"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"));

            verify(authApi).getCurrentUser(any());
        }

        @Test
        @DisplayName("returns 401 when not authenticated")
        void notAuthenticated() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());

            verify(authApi).getCurrentUser(any());
        }
    }
}
