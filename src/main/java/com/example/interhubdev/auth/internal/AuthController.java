package com.example.interhubdev.auth.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.auth.AuthResult;
import com.example.interhubdev.auth.ForgotPasswordRequest;
import com.example.interhubdev.auth.ForgotPasswordResponse;
import com.example.interhubdev.auth.LoginRequest;
import com.example.interhubdev.auth.ResetPasswordRequest;
import com.example.interhubdev.auth.ResetPasswordResponse;
import com.example.interhubdev.user.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, and token management")
class AuthController {

    private final AuthApi authApi;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and set JWT cookies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account not active")
    })
    public ResponseEntity<AuthResult> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthResult result = authApi.login(
                request.email(),
                request.password(),
                httpRequest,
                httpResponse
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Get new access token using refresh token cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<AuthResult> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        AuthResult result = authApi.refresh(request, response);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke refresh token and clear cookies")
    @ApiResponse(responseCode = "204", description = "Logged out successfully")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        authApi.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Get currently authenticated user info")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User info"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserDto> getCurrentUser(HttpServletRequest request) {
        return authApi.getCurrentUser(request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Send OTP to email if account exists and is active. Always returns 202.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Request accepted (email sent if account exists)"),
            @ApiResponse(responseCode = "429", description = "Too many requests (OTP rate limit)")
    })
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authApi.requestPasswordReset(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ForgotPasswordResponse("Если аккаунт с таким email существует, на него будет отправлено письмо с инструкциями."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with OTP", description = "Verify OTP code and set new password. All sessions are revoked.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired code")
    })
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authApi.resetPassword(request.email(), request.code(), request.newPassword());
        return ResponseEntity.ok(new ResetPasswordResponse("Пароль успешно изменён. Войдите с новым паролем."));
    }
}
