package com.unishare.api.modules.auth.service.impl;

import com.unishare.api.common.constants.Roles;
import com.unishare.api.common.constants.UserStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.infrastructure.security.JwtService;
import com.unishare.api.modules.auth.dto.request.LoginRequest;
import com.unishare.api.modules.auth.dto.request.RefreshTokenRequest;
import com.unishare.api.modules.auth.dto.request.RegisterRequest;
import com.unishare.api.modules.auth.dto.response.AuthResponse;
import com.unishare.api.modules.auth.entity.*;
import com.unishare.api.modules.auth.exception.AuthErrorCode;
import com.unishare.api.modules.auth.repository.*;
import com.unishare.api.modules.user.dto.UserProfileResponse;
import com.unishare.api.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserCredentialRepository userCredentialRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private OtpTokenRepository otpTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private UserService userService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "emailVerificationPageUrl", "http://localhost:3000/verify");
        ReflectionTestUtils.setField(authService, "passwordResetPageUrl", "http://localhost:3000/reset");

        userRole = new Role();
        userRole.setId(UUID.randomUUID());
        userRole.setName(Roles.USER);

        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("test@example.com");
        sampleUser.setEmailVerified(true);
        sampleUser.setStatus(UserStatuses.ACTIVE);

        UserRole ur = new UserRole();
        ur.setRole(userRole);
        ur.getId().setRoleId(userRole.getId());
        ur.getId().setUserId(sampleUser.getId());
        sampleUser.addUserRole(ur);
    }

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        @DisplayName("Đăng ký thành công")
        void register_Success() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@example.com");
            req.setPassword("pass123");
            req.setFirstName("First");
            req.setLastName("Last");

            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
            when(roleRepository.findByName(Roles.USER)).thenReturn(Optional.of(userRole));
            
            User savedUser = new User();
            savedUser.setId(UUID.randomUUID());
            savedUser.setEmail(req.getEmail());
            UserRole newUr = new UserRole();
            newUr.setRole(userRole);
            newUr.getId().setRoleId(userRole.getId());
            newUr.getId().setUserId(savedUser.getId());
            savedUser.addUserRole(newUr);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserProfileResponse profileResp = new UserProfileResponse();
            profileResp.setFirstName("First");
            profileResp.setLastName("Last");
            when(userService.getProfile(savedUser.getId())).thenReturn(profileResp);

            AuthResponse resp = authService.register(req);

            assertNotNull(resp);
            assertEquals(savedUser.getId(), resp.getUserId());
            assertEquals(req.getEmail(), resp.getEmail());
            assertNull(resp.getAccessToken()); // Register returns empty token till verified

            verify(userService).createProfileForNewUser(savedUser.getId(), "First", "Last");
            verify(otpTokenRepository).save(any(OtpToken.class));
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Email đã tồn tại")
        void register_EmailExists() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("existing@example.com");

            when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

            AppException ex = assertThrows(AppException.class, () -> authService.register(req));
            assertEquals(AuthErrorCode.EMAIL_ALREADY_EXISTS, ex.getExceptionCode());
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Đăng nhập thành công")
        void login_Success() {
            LoginRequest req = new LoginRequest();
            req.setEmail("test@example.com");
            req.setPassword("pass123");

            UserCredential cred = new UserCredential();
            cred.setPasswordHash("hashed_pass");

            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(sampleUser));
            when(userCredentialRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(cred));
            when(passwordEncoder.matches("pass123", "hashed_pass")).thenReturn(true);

            UserProfileResponse profileResp = new UserProfileResponse();
            when(userService.getProfile(sampleUser.getId())).thenReturn(profileResp);

            when(jwtService.generateAccessToken(eq(sampleUser.getId()), anyList())).thenReturn("access-token");
            when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
            when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);
            when(jwtService.getRefreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(86400));

            AuthResponse resp = authService.login(req, "127.0.0.1", "UserAgent/1.0");

            assertNotNull(resp);
            assertEquals("access-token", resp.getAccessToken());
            assertEquals("refresh-token", resp.getRefreshToken());
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Sai mật khẩu")
        void login_InvalidPassword() {
            LoginRequest req = new LoginRequest();
            req.setEmail("test@example.com");
            req.setPassword("wrongpass");

            UserCredential cred = new UserCredential();
            cred.setPasswordHash("hashed_pass");

            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(sampleUser));
            when(userCredentialRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(cred));
            when(passwordEncoder.matches("wrongpass", "hashed_pass")).thenReturn(false);

            AppException ex = assertThrows(AppException.class, () -> authService.login(req, null, null));
            assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getExceptionCode());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Email chưa xác minh")
        void login_EmailNotVerified() {
            sampleUser.setEmailVerified(false);
            LoginRequest req = new LoginRequest();
            req.setEmail("test@example.com");
            req.setPassword("pass123");

            UserCredential cred = new UserCredential();
            cred.setPasswordHash("hashed");

            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(sampleUser));
            when(userCredentialRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(cred));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            AppException ex = assertThrows(AppException.class, () -> authService.login(req, null, null));
            assertEquals(AuthErrorCode.EMAIL_NOT_VERIFIED, ex.getExceptionCode());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Tài khoản bị khoá")
        void login_AccountDisabled() {
            sampleUser.setStatus(UserStatuses.SUSPENDED);
            LoginRequest req = new LoginRequest();
            req.setEmail("test@example.com");
            req.setPassword("pass123");

            UserCredential cred = new UserCredential();
            cred.setPasswordHash("hashed");

            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(sampleUser));
            when(userCredentialRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(cred));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            AppException ex = assertThrows(AppException.class, () -> authService.login(req, null, null));
            assertEquals(AuthErrorCode.ACCOUNT_DISABLED, ex.getExceptionCode());
        }
    }

    @Nested
    @DisplayName("RefreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("Refresh token thành công")
        void refreshToken_Success() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("old-refresh-token");

            RefreshToken storedRt = new RefreshToken();
            storedRt.setUserId(sampleUser.getId());
            storedRt.setToken("old-refresh-token");
            storedRt.setRevoked(false);
            storedRt.setExpiresAt(Instant.now().plusSeconds(3600));

            when(refreshTokenRepository.findByTokenAndRevokedFalse("old-refresh-token")).thenReturn(Optional.of(storedRt));
            when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
            
            UserProfileResponse profileResp = new UserProfileResponse();
            when(userService.getProfile(sampleUser.getId())).thenReturn(profileResp);
            
            when(jwtService.generateAccessToken(eq(sampleUser.getId()), anyList())).thenReturn("new-access-token");

            AuthResponse resp = authService.refreshToken(req, "127.0.0.1", "NewUserAgent/2.0");

            assertNotNull(resp);
            assertEquals("new-access-token", resp.getAccessToken());
            assertEquals("old-refresh-token", resp.getRefreshToken()); // Reuse same RT string
            assertEquals("127.0.0.1", storedRt.getIpAddress());
            verify(refreshTokenRepository).save(storedRt);
        }

        @Test
        @DisplayName("Refresh token thất bại - Token hết hạn")
        void refreshToken_Expired() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("expired-token");

            RefreshToken storedRt = new RefreshToken();
            storedRt.setToken("expired-token");
            storedRt.setRevoked(false);
            storedRt.setExpiresAt(Instant.now().minusSeconds(10)); // Expired

            when(refreshTokenRepository.findByTokenAndRevokedFalse("expired-token")).thenReturn(Optional.of(storedRt));

            AppException ex = assertThrows(AppException.class, () -> authService.refreshToken(req, null, null));
            assertEquals(AuthErrorCode.TOKEN_EXPIRED, ex.getExceptionCode());
            assertTrue(storedRt.getRevoked());
            verify(refreshTokenRepository).save(storedRt);
        }
    }
}
