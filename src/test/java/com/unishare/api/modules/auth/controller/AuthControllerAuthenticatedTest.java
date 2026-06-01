package com.unishare.api.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.config.GlobalExceptionHandler;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.auth.dto.request.ChangePasswordRequest;
import com.unishare.api.modules.auth.dto.request.RefreshTokenRequest;
import com.unishare.api.modules.auth.dto.response.MeResponse;
import com.unishare.api.modules.auth.dto.response.SessionResponse;
import com.unishare.api.modules.auth.exception.AuthErrorCode;
import com.unishare.api.modules.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests cho {@link AuthController} — các endpoint YÊU CẦU AUTHENTICATION
 * (logout, me, change-password, sessions, revoke-session, revoke-all).
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerAuthenticatedTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private UUID userId;
    private CustomUserPrincipal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        principal = new CustomUserPrincipal(
                userId, "testuser@gmail.com", "hashedpw",
                List.of("USER"),
                List.of("VIEW_PROFILE", "UPDATE_PROFILE"),
                true
        );

        objectMapper = new ObjectMapper();

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(CustomUserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return principal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // =========================================================================
    // LOGOUT
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Đăng xuất thành công — 200")
        void logout_Success_Returns200() throws Exception {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("valid-refresh-token");

            doNothing().when(authService).logout(any(RefreshTokenRequest.class));

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đăng xuất thành công."))
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }

        @Test
        @DisplayName("Refresh token trống — 400")
        void logout_BlankToken_Returns400() throws Exception {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("");

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.fields.refreshToken").exists());
        }

        @Test
        @DisplayName("Token không hợp lệ — 401")
        void logout_InvalidToken_Returns401() throws Exception {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("invalid");

            doThrow(new AppException(AuthErrorCode.INVALID_TOKEN))
                    .when(authService).logout(any(RefreshTokenRequest.class));

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errors.type").value("INVALID_TOKEN"));
        }
    }

    // =========================================================================
    // ME
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class MeTests {

        @Test
        @DisplayName("Lấy thông tin phiên — 200")
        void getMe_Success_Returns200() throws Exception {
            MeResponse meResp = MeResponse.builder()
                    .userId(userId)
                    .email("testuser@gmail.com")
                    .emailVerified(true)
                    .status("ACTIVE")
                    .firstName("Test")
                    .lastName("User")
                    .fullName("Test User")
                    .roles(List.of("USER"))
                    .createdAt(Instant.now())
                    .build();

            when(authService.getMe(userId)).thenReturn(meResp);

            mockMvc.perform(get("/api/v1/auth/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.data.email").value("testuser@gmail.com"))
                    .andExpect(jsonPath("$.data.emailVerified").value(true))
                    .andExpect(jsonPath("$.data.firstName").value("Test"))
                    .andExpect(jsonPath("$.data.roles[0]").value("USER"))
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }

        @Test
        @DisplayName("User không tồn tại — 404")
        void getMe_UserNotFound_Returns404() throws Exception {
            when(authService.getMe(userId))
                    .thenThrow(new AppException(AuthErrorCode.USER_NOT_FOUND));

            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors.type").value("USER_NOT_FOUND"))
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    // =========================================================================
    // CHANGE PASSWORD
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/auth/change-password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Đổi mật khẩu thành công — 200")
        void changePassword_Success_Returns200() throws Exception {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("oldPass123");
            req.setNewPassword("newPass456");

            doNothing().when(authService).changePassword(eq(userId), any(ChangePasswordRequest.class), any());

            mockMvc.perform(post("/api/v1/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Refresh-Token", "current-rt")
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }

        @Test
        @DisplayName("Thiếu currentPassword — 400")
        void changePassword_BlankCurrent_Returns400() throws Exception {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("");
            req.setNewPassword("newPass456");

            mockMvc.perform(post("/api/v1/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.fields.currentPassword").exists());
        }

        @Test
        @DisplayName("Mật khẩu mới < 8 ký tự — 400")
        void changePassword_ShortNewPassword_Returns400() throws Exception {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("oldPass123");
            req.setNewPassword("short");

            mockMvc.perform(post("/api/v1/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.fields.newPassword").exists());
        }

        @Test
        @DisplayName("Mật khẩu hiện tại sai — 400 INVALID_CURRENT_PASSWORD")
        void changePassword_WrongCurrent_Returns400() throws Exception {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("wrongPass");
            req.setNewPassword("newPass456");

            doThrow(new AppException(AuthErrorCode.INVALID_CURRENT_PASSWORD))
                    .when(authService).changePassword(eq(userId), any(ChangePasswordRequest.class), any());

            mockMvc.perform(post("/api/v1/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.type").value("INVALID_CURRENT_PASSWORD"));
        }

        @Test
        @DisplayName("Không gửi X-Refresh-Token header — vẫn OK (optional)")
        void changePassword_NoRefreshTokenHeader_Returns200() throws Exception {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("oldPass123");
            req.setNewPassword("newPass456");

            doNothing().when(authService).changePassword(eq(userId), any(ChangePasswordRequest.class), any());

            mockMvc.perform(post("/api/v1/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }

    // =========================================================================
    // SESSIONS
    // =========================================================================
    @Nested
    @DisplayName("Session management APIs")
    class SessionTests {

        @Test
        @DisplayName("GET /sessions — Liệt kê phiên đang hoạt động — 200")
        void listSessions_Success_Returns200() throws Exception {
            SessionResponse session = SessionResponse.builder()
                    .id(UUID.randomUUID())
                    .deviceInfo("Chrome / Windows")
                    .ipAddress("192.168.1.1")
                    .userAgent("Mozilla/5.0")
                    .createdAt(Instant.now())
                    .lastUsedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .current(true)
                    .build();

            when(authService.listSessions(eq(userId), any())).thenReturn(List.of(session));

            mockMvc.perform(get("/api/v1/auth/sessions")
                            .header("X-Refresh-Token", "current-rt"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].deviceInfo").value("Chrome / Windows"))
                    .andExpect(jsonPath("$.data[0].current").value(true))
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }

        @Test
        @DisplayName("GET /sessions — Không có phiên nào — 200 mảng rỗng")
        void listSessions_Empty_Returns200() throws Exception {
            when(authService.listSessions(eq(userId), any())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/auth/sessions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("DELETE /sessions/{id} — Thu hồi phiên thành công — 200")
        void revokeSession_Success_Returns200() throws Exception {
            UUID sessionId = UUID.randomUUID();
            doNothing().when(authService).revokeSession(userId, sessionId);

            mockMvc.perform(delete("/api/v1/auth/sessions/{id}", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đã thu hồi phiên đăng nhập."))
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }

        @Test
        @DisplayName("DELETE /sessions/{id} — Phiên không tồn tại — 404")
        void revokeSession_NotFound_Returns404() throws Exception {
            UUID sessionId = UUID.randomUUID();
            doThrow(new AppException(AuthErrorCode.SESSION_NOT_FOUND))
                    .when(authService).revokeSession(userId, sessionId);

            mockMvc.perform(delete("/api/v1/auth/sessions/{id}", sessionId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors.type").value("SESSION_NOT_FOUND"))
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("POST /sessions/revoke-all — Đăng xuất mọi thiết bị — 200")
        void revokeAll_Success_Returns200() throws Exception {
            doNothing().when(authService).revokeAllSessions(userId);

            mockMvc.perform(post("/api/v1/auth/sessions/revoke-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đã đăng xuất khỏi mọi thiết bị."))
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }
}
