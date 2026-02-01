package com.example.interhubdev.auth.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.auth.AuthApi.AuthenticationException;
import com.example.interhubdev.auth.AuthApi.AuthErrorCode;
import com.example.interhubdev.auth.AuthResult;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "user@test.com";
    private static final String PASSWORD = "password123";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String TOKEN_HASH = "sha256-hash";

    @Mock
    private UserApi userApi;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthServiceImpl authService;

    private static UserDto activeUser() {
        return new UserDto(
                USER_ID, EMAIL, Role.STUDENT, UserStatus.ACTIVE,
                "John", "Doe", null, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns AuthResult and sets cookies when credentials valid")
        void success() {
            UserDto user = activeUser();
            when(userApi.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userApi.verifyPassword(EMAIL, PASSWORD)).thenReturn(true);
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken()).thenReturn(REFRESH_TOKEN);
            when(jwtService.hashToken(REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
            when(jwtService.getRefreshTokenExpiry()).thenReturn(LocalDateTime.now().plusDays(7));
            when(jwtService.getAccessTokenMaxAge()).thenReturn(900);
            when(jwtService.getRefreshTokenMaxAge()).thenReturn(604800);
            when(cookieHelper.getUserAgent(request)).thenReturn("Mozilla/5.0");
            when(cookieHelper.getClientIp(request)).thenReturn("127.0.0.1");

            AuthResult result = authService.login(EMAIL, PASSWORD, request, response);

            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.role()).isEqualTo(Role.STUDENT);
            assertThat(result.fullName()).isEqualTo("John Doe");
            assertThat(result.message()).isEqualTo("Login successful");

            verify(refreshTokenRepository).save(any(RefreshToken.class));
            verify(userApi).updateLastLoginAt(USER_ID);
            verify(cookieHelper).setAccessTokenCookie(response, ACCESS_TOKEN, 900);
            verify(cookieHelper).setRefreshTokenCookie(response, REFRESH_TOKEN, 604800);
        }

        @Test
        @DisplayName("throws INVALID_CREDENTIALS when user not found")
        void userNotFound() {
            when(userApi.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, request, response))
                    .isInstanceOf(AuthenticationException.class)
                    .satisfies(ex -> assertThat(((AuthenticationException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));

            verify(userApi, never()).verifyPassword(anyString(), anyString());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws USER_NOT_ACTIVE when user status is PENDING")
        void userPending() {
            UserDto user = new UserDto(
                    USER_ID, EMAIL, Role.STUDENT, UserStatus.PENDING,
                    null, null, null, null,
                    LocalDateTime.now(), null, null
            );
            when(userApi.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, request, response))
                    .isInstanceOf(AuthenticationException.class)
                    .satisfies(ex -> assertThat(((AuthenticationException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.USER_NOT_ACTIVE));

            verify(userApi, never()).verifyPassword(anyString(), anyString());
        }

        @Test
        @DisplayName("throws USER_DISABLED when user status is DISABLED")
        void userDisabled() {
            UserDto user = new UserDto(
                    USER_ID, EMAIL, Role.STUDENT, UserStatus.DISABLED,
                    null, null, null, null,
                    LocalDateTime.now(), null, null
            );
            when(userApi.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, request, response))
                    .isInstanceOf(AuthenticationException.class)
                    .satisfies(ex -> assertThat(((AuthenticationException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.USER_DISABLED));

            verify(userApi, never()).verifyPassword(anyString(), anyString());
        }

        @Test
        @DisplayName("throws INVALID_CREDENTIALS when password wrong")
        void wrongPassword() {
            UserDto user = activeUser();
            when(userApi.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userApi.verifyPassword(EMAIL, PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, request, response))
                    .isInstanceOf(AuthenticationException.class)
                    .satisfies(ex -> assertThat(((AuthenticationException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("returns new AuthResult and sets new cookies when refresh token valid")
        void success() {
            UserDto user = activeUser();
            RefreshToken tokenEntity = RefreshToken.builder()
                    .userId(USER_ID)
                    .tokenHash(TOKEN_HASH)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .revoked(false)
                    .build();
            when(cookieHelper.getRefreshToken(request)).thenReturn(Optional.of(REFRESH_TOKEN));
            when(jwtService.hashToken(REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(tokenEntity));
            when(userApi.findById(USER_ID)).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken()).thenReturn("new-refresh-token");
            when(jwtService.hashToken("new-refresh-token")).thenReturn("new-hash");
            when(jwtService.getRefreshTokenExpiry()).thenReturn(LocalDateTime.now().plusDays(7));
            when(jwtService.getAccessTokenMaxAge()).thenReturn(900);
            when(jwtService.getRefreshTokenMaxAge()).thenReturn(604800);
            when(cookieHelper.getUserAgent(request)).thenReturn("Mozilla/5.0");
            when(cookieHelper.getClientIp(request)).thenReturn("127.0.0.1");

            AuthResult result = authService.refresh(request, response);

            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.email()).isEqualTo(EMAIL);
            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
            verify(cookieHelper).setAccessTokenCookie(response, ACCESS_TOKEN, 900);
        }

        @Test
        @DisplayName("throws TOKEN_INVALID when refresh token cookie missing")
        void noRefreshToken() {
            when(cookieHelper.getRefreshToken(request)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(request, response))
                    .isInstanceOf(AuthenticationException.class)
                    .satisfies(ex -> assertThat(((AuthenticationException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.TOKEN_INVALID));
        }

        @Test
        @DisplayName("throws TOKEN_INVALID when token not in database")
        void tokenNotInDb() {
            when(cookieHelper.getRefreshToken(request)).thenReturn(Optional.of(REFRESH_TOKEN));
            when(jwtService.hashToken(REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(request, response))
                    .isInstanceOf(AuthenticationException.class)
                    .satisfies(ex -> assertThat(((AuthenticationException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.TOKEN_INVALID));
        }

        @Test
        @DisplayName("throws TOKEN_EXPIRED when token revoked")
        void tokenRevoked() {
            RefreshToken tokenEntity = RefreshToken.builder()
                    .userId(USER_ID)
                    .tokenHash(TOKEN_HASH)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .revoked(true)
                    .build();
            when(cookieHelper.getRefreshToken(request)).thenReturn(Optional.of(REFRESH_TOKEN));
            when(jwtService.hashToken(REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(tokenEntity));

            assertThatThrownBy(() -> authService.refresh(request, response))
                    .isInstanceOf(AuthenticationException.class)
                    .satisfies(ex -> assertThat(((AuthenticationException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.TOKEN_EXPIRED));

            verify(cookieHelper).clearAuthCookies(response);
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND when user deleted")
        void userNotFound() {
            RefreshToken tokenEntity = RefreshToken.builder()
                    .userId(USER_ID)
                    .tokenHash(TOKEN_HASH)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .revoked(false)
                    .build();
            when(cookieHelper.getRefreshToken(request)).thenReturn(Optional.of(REFRESH_TOKEN));
            when(jwtService.hashToken(REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(tokenEntity));
            when(userApi.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(request, response))
                    .isInstanceOf(AuthenticationException.class)
                    .satisfies(ex -> assertThat(((AuthenticationException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("revokes token and clears cookies when refresh token present")
        void withToken() {
            RefreshToken tokenEntity = RefreshToken.builder()
                    .userId(USER_ID)
                    .tokenHash(TOKEN_HASH)
                    .revoked(false)
                    .build();
            when(cookieHelper.getRefreshToken(request)).thenReturn(Optional.of(REFRESH_TOKEN));
            when(jwtService.hashToken(REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(tokenEntity));

            authService.logout(request, response);

            verify(refreshTokenRepository).save(argThat(RefreshToken::isRevoked));
            verify(cookieHelper).clearAuthCookies(response);
        }

        @Test
        @DisplayName("clears cookies only when no refresh token")
        void withoutToken() {
            when(cookieHelper.getRefreshToken(request)).thenReturn(Optional.empty());

            authService.logout(request, response);

            verify(refreshTokenRepository, never()).save(any());
            verify(cookieHelper).clearAuthCookies(response);
        }
    }

    @Nested
    @DisplayName("logoutAll")
    class LogoutAll {

        @Test
        @DisplayName("revokes all user tokens and clears cookies")
        void success() {
            when(refreshTokenRepository.revokeAllByUserId(eq(USER_ID), any(LocalDateTime.class))).thenReturn(3);

            authService.logoutAll(USER_ID, response);

            verify(refreshTokenRepository).revokeAllByUserId(eq(USER_ID), any(LocalDateTime.class));
            verify(cookieHelper).clearAuthCookies(response);
        }
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName("returns user when valid access token in cookie")
        void validToken() {
            UserDto user = activeUser();
            JwtService.TokenClaims claims = new JwtService.TokenClaims(USER_ID, EMAIL, Role.STUDENT, "John Doe");
            when(cookieHelper.getAccessToken(request)).thenReturn(Optional.of(ACCESS_TOKEN));
            when(jwtService.validateAccessToken(ACCESS_TOKEN)).thenReturn(Optional.of(claims));
            when(userApi.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThat(authService.getCurrentUser(request)).contains(user);
        }

        @Test
        @DisplayName("returns empty when no access token")
        void noToken() {
            when(cookieHelper.getAccessToken(request)).thenReturn(Optional.empty());

            assertThat(authService.getCurrentUser(request)).isEmpty();
            verify(jwtService, never()).validateAccessToken(anyString());
        }

        @Test
        @DisplayName("returns empty when token invalid")
        void invalidToken() {
            when(cookieHelper.getAccessToken(request)).thenReturn(Optional.of("bad-token"));
            when(jwtService.validateAccessToken("bad-token")).thenReturn(Optional.empty());

            assertThat(authService.getCurrentUser(request)).isEmpty();
            verify(userApi, never()).findById(any());
        }
    }
}
