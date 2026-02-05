package com.example.interhubdev.auth.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.auth.AuthApi.AuthenticationException;
import com.example.interhubdev.auth.AuthApi.AuthErrorCode;
import com.example.interhubdev.auth.AuthResult;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of authentication service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class AuthServiceImpl implements AuthApi {

    private final UserApi userApi;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieHelper cookieHelper;
    private final LoginRateLimitService loginRateLimitService;

    @Override
    @Transactional
    public AuthResult login(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        log.debug("Login attempt for email: {}", email);

        String clientIp = cookieHelper.getClientIp(request);
        if (!loginRateLimitService.tryAcquire(clientIp)) {
            throw new AuthenticationException(AuthErrorCode.TOO_MANY_REQUESTS, "Too many login attempts. Try again later.");
        }

        // Find user
        UserDto user = userApi.findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("User not found: {}", email);
                    loginRateLimitService.recordFailedAttempt(clientIp);
                    return new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
                });

        // Check user status
        if (user.status() == UserStatus.PENDING) {
            log.debug("User {} has PENDING status - account not activated", email);
            loginRateLimitService.recordFailedAttempt(clientIp);
            throw new AuthenticationException(AuthErrorCode.USER_NOT_ACTIVE, "Account not activated. Please check your email for activation link.");
        }

        if (user.status() == UserStatus.DISABLED) {
            log.debug("User {} is DISABLED", email);
            loginRateLimitService.recordFailedAttempt(clientIp);
            throw new AuthenticationException(AuthErrorCode.USER_DISABLED, "Account is disabled. Please contact administrator.");
        }

        // Verify password
        if (!userApi.verifyPassword(email, password)) {
            log.debug("Invalid password for user: {}", email);
            loginRateLimitService.recordFailedAttempt(clientIp);
            throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        // Save refresh token to database
        RefreshToken tokenEntity = RefreshToken.builder()
                .userId(user.id())
                .tokenHash(jwtService.hashToken(refreshToken))
                .expiresAt(jwtService.getRefreshTokenExpiry())
                .userAgent(cookieHelper.getUserAgent(request))
                .ipAddress(cookieHelper.getClientIp(request))
                .build();
        refreshTokenRepository.save(tokenEntity);

        // Update last login time
        userApi.updateLastLoginAt(user.id());

        // Set cookies
        cookieHelper.setAccessTokenCookie(response, accessToken, jwtService.getAccessTokenMaxAge());
        cookieHelper.setRefreshTokenCookie(response, refreshToken, jwtService.getRefreshTokenMaxAge());

        log.info("User {} logged in successfully", email);
        return AuthResult.success(user.id(), user.email(),
                user.roles() != null ? List.copyOf(user.roles()) : List.of(),
                user.getFullName());
    }

    @Override
    @Transactional
    public AuthResult refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieHelper.getRefreshToken(request)
                .orElseThrow(() -> new AuthenticationException(AuthErrorCode.TOKEN_INVALID, "Refresh token not found"));

        String tokenHash = jwtService.hashToken(refreshToken);

        RefreshToken tokenEntity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.debug("Refresh token not found in database");
                    return new AuthenticationException(AuthErrorCode.TOKEN_INVALID, "Invalid refresh token");
                });

        if (!tokenEntity.isValid()) {
            log.debug("Refresh token is expired or revoked for user {}", tokenEntity.getUserId());
            cookieHelper.clearAuthCookies(response);
            throw new AuthenticationException(AuthErrorCode.TOKEN_EXPIRED, "Refresh token expired. Please login again.");
        }

        // Get user
        UserDto user = userApi.findById(tokenEntity.getUserId())
                .orElseThrow(() -> {
                    log.error("User not found for valid refresh token: {}", tokenEntity.getUserId());
                    return new AuthenticationException(AuthErrorCode.USER_NOT_FOUND, "User not found");
                });

        // Check user is still active
        if (!user.canLogin()) {
            log.debug("User {} cannot login - status: {}", user.email(), user.status());
            tokenEntity.revoke();
            refreshTokenRepository.save(tokenEntity);
            cookieHelper.clearAuthCookies(response);
            throw new AuthenticationException(AuthErrorCode.USER_DISABLED, "Account is disabled");
        }

        // Revoke old refresh token and create new one (rotation)
        tokenEntity.revoke();
        refreshTokenRepository.save(tokenEntity);

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken();

        // Save new refresh token
        RefreshToken newTokenEntity = RefreshToken.builder()
                .userId(user.id())
                .tokenHash(jwtService.hashToken(newRefreshToken))
                .expiresAt(jwtService.getRefreshTokenExpiry())
                .userAgent(cookieHelper.getUserAgent(request))
                .ipAddress(cookieHelper.getClientIp(request))
                .build();
        refreshTokenRepository.save(newTokenEntity);

        // Set new cookies
        cookieHelper.setAccessTokenCookie(response, newAccessToken, jwtService.getAccessTokenMaxAge());
        cookieHelper.setRefreshTokenCookie(response, newRefreshToken, jwtService.getRefreshTokenMaxAge());

        log.debug("Tokens refreshed for user {}", user.email());
        return AuthResult.success(user.id(), user.email(),
                user.roles() != null ? List.copyOf(user.roles()) : List.of(),
                user.getFullName());
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cookieHelper.getRefreshToken(request)
                .ifPresent(token -> {
                    String tokenHash = jwtService.hashToken(token);
                    refreshTokenRepository.findByTokenHash(tokenHash)
                            .ifPresent(tokenEntity -> {
                                tokenEntity.revoke();
                                refreshTokenRepository.save(tokenEntity);
                                log.debug("Refresh token revoked for user {}", tokenEntity.getUserId());
                            });
                });

        cookieHelper.clearAuthCookies(response);
        log.debug("User logged out");
    }

    @Override
    @Transactional
    public void logoutAll(UUID userId, HttpServletResponse response) {
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
        cookieHelper.clearAuthCookies(response);
        log.info("Revoked {} refresh tokens for user {}", revokedCount, userId);
    }

    @Override
    @Transactional
    public void revokeAllTokensForUser(UUID userId) {
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
        log.info("Revoked {} refresh tokens for user {}", revokedCount, userId);
    }

    @Override
    public Optional<UserDto> getCurrentUser(HttpServletRequest request) {
        return cookieHelper.getAccessToken(request)
                .flatMap(jwtService::validateAccessToken)
                .flatMap(claims -> userApi.findById(claims.userId()));
    }
}
