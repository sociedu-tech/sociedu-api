package com.unishare.api.modules.auth.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.auth.dto.request.*;
import com.unishare.api.modules.auth.dto.response.AuthResponse;
import com.unishare.api.modules.auth.dto.response.MeResponse;
import com.unishare.api.modules.auth.dto.response.SessionResponse;
import com.unishare.api.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";

    private final AuthService authService;

    @Operation(summary = "Đăng ký")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse data = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AuthResponse>build()
                        .withHttpStatus(HttpStatus.CREATED)
                        .withData(data)
                        .withMessage("Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản."));
    }

    @Operation(summary = "Đăng nhập")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        AuthResponse data = authService.login(request, clientIp(servletRequest),
                servletRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.<AuthResponse>build()
                .withData(data)
                .withMessage("Đăng nhập thành công."));
    }

    /** POST /api/v1/auth/refresh — Làm mới Access Token, xoay Refresh Token. */
    @Operation(summary = "Làm mới token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest) {
        AuthResponse data = authService.refreshToken(request, clientIp(servletRequest),
                servletRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.<AuthResponse>build().withData(data));
    }

    @Operation(summary = "Đăng xuất")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.<Void>build().withMessage("Đăng xuất thành công."));
    }

    /** POST /api/v1/auth/verify-email — Xác minh email bằng token từ liên kết trong mail. */
    @Operation(summary = "Xác minh email")
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        AuthResponse data = authService.verifyEmail(request.getToken());
        return ResponseEntity.ok(ApiResponse.<AuthResponse>build()
                .withData(data)
                .withMessage("Xác minh email thành công."));
    }

    /** POST /api/v1/auth/resend-verification — Gửi lại email chứa liên kết xác minh. */
    @Operation(summary = "Gửi lại email xác minh")
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        authService.sendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.<Void>build()
                .withMessage("Nếu email tồn tại và chưa xác minh, email chứa liên kết đã được gửi."));
    }

    @Operation(summary = "Quên mật khẩu")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>build()
                .withMessage("Nếu email tồn tại, liên kết đặt lại mật khẩu đã được gửi."));
    }

    @Operation(summary = "Đặt lại mật khẩu")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>build().withMessage("Đặt lại mật khẩu thành công."));
    }

    /** GET /api/v1/auth/me — Thông tin phiên hiện tại (user + roles). */
    @Operation(summary = "Thông tin phiên hiện tại")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        MeResponse data = authService.getMe(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.<MeResponse>build().withData(data));
    }

    /** POST /api/v1/auth/change-password — Đổi mật khẩu (giữ lại phiên hiện tại, revoke phiên khác). */
    @Operation(summary = "Đổi mật khẩu")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String currentRefreshToken) {
        authService.changePassword(principal.getUserId(), request, currentRefreshToken);
        return ResponseEntity.ok(ApiResponse.<Void>build()
                .withMessage("Đổi mật khẩu thành công. Phiên đăng nhập ở thiết bị khác đã bị thu hồi."));
    }

    /** GET /api/v1/auth/sessions — Liệt kê phiên đang hoạt động. */
    @Operation(summary = "Danh sách phiên đang hoạt động")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> listSessions(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String currentRefreshToken) {
        List<SessionResponse> data = authService.listSessions(principal.getUserId(), currentRefreshToken);
        return ResponseEntity.ok(ApiResponse.<List<SessionResponse>>build().withData(data));
    }

    /** DELETE /api/v1/auth/sessions/{id} — Revoke 1 phiên cụ thể. */
    @Operation(summary = "Thu hồi phiên đăng nhập")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID sessionId) {
        authService.revokeSession(principal.getUserId(), sessionId);
        return ResponseEntity.ok(ApiResponse.<Void>build().withMessage("Đã thu hồi phiên đăng nhập."));
    }

    /** POST /api/v1/auth/sessions/revoke-all — Đăng xuất khỏi toàn bộ thiết bị. */
    @Operation(summary = "Đăng xuất khỏi mọi thiết bị")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/sessions/revoke-all")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        authService.revokeAllSessions(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.<Void>build()
                .withMessage("Đã đăng xuất khỏi mọi thiết bị."));
    }

    // =========================================================================
    // FLOW A — Xác thực số điện thoại (Authenticated)
    // =========================================================================

    /** POST /api/v1/auth/phone/send-otp — Gửi OTP tới email để xác thực phone. */
    @Operation(summary = "Gửi OTP xác thực số điện thoại (qua email)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/phone/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendPhoneOtp(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody SendPhoneOtpRequest request) {
        authService.sendPhoneVerificationOtp(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.<Void>build()
                .withMessage("Mã OTP đã được gửi tới email của bạn."));
    }

    /** POST /api/v1/auth/phone/verify-otp — Xác thực OTP và gắn phone vào tài khoản. */
    @Operation(summary = "Xác thực OTP để gắn số điện thoại")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/phone/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyPhoneOtp(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody VerifyPhoneOtpRequest request) {
        authService.verifyPhoneOtp(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.<Void>build()
                .withMessage("Xác thực số điện thoại thành công."));
    }

    // =========================================================================
    // FLOW C — Đăng nhập bằng email OTP (Public)
    // =========================================================================

    /** POST /api/v1/auth/otp/send — Gửi OTP đăng nhập tới email. */
    @Operation(summary = "Gửi mã OTP đăng nhập qua email")
    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<Void>> sendLoginOtp(
            @Valid @RequestBody SendLoginOtpRequest request) {
        authService.sendLoginOtp(request);
        return ResponseEntity.ok(ApiResponse.<Void>build()
                .withMessage("Nếu email hợp lệ, mã OTP đã được gửi."));
    }

    /** POST /api/v1/auth/otp/login — Đăng nhập bằng email + OTP. */
    @Operation(summary = "Đăng nhập bằng email OTP")
    @PostMapping("/otp/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithOtp(
            @Valid @RequestBody LoginOtpRequest request,
            HttpServletRequest servletRequest) {
        AuthResponse data = authService.loginWithOtp(request, clientIp(servletRequest),
                servletRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.<AuthResponse>build()
                .withData(data)
                .withMessage("Đăng nhập thành công."));
    }

    /** Trích client IP, ưu tiên header forwarded (reverse proxy). */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}
