package com.unishare.api.modules.auth.service.impl;

import com.unishare.api.common.constants.UserStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.event.LoginOtpMailEvent;
import com.unishare.api.common.event.PhoneVerificationOtpMailEvent;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.infrastructure.security.JwtService;
import com.unishare.api.modules.auth.dto.request.LoginOtpRequest;
import com.unishare.api.modules.auth.dto.request.SendLoginOtpRequest;
import com.unishare.api.modules.auth.dto.request.SendPhoneOtpRequest;
import com.unishare.api.modules.auth.dto.request.VerifyPhoneOtpRequest;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho OTP flows trong {@link AuthServiceImpl}.
 * Flow A: sendPhoneVerificationOtp / verifyPhoneOtp
 * Flow C: sendLoginOtp / loginWithOtp
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceOtpFlowTest {

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

    private User activeUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "emailVerificationPageUrl", "http://localhost:3000/verify");
        ReflectionTestUtils.setField(authService, "passwordResetPageUrl", "http://localhost:3000/reset");

        activeUser = new User();
        activeUser.setId(UUID.randomUUID());
        activeUser.setEmail("test@example.com");
        activeUser.setEmailVerified(true);
        activeUser.setStatus(UserStatuses.ACTIVE);
        activeUser.setPhoneVerified(false);

        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("USER");
        UserRole ur = new UserRole();
        ur.setRole(role);
        ur.getId().setRoleId(role.getId());
        ur.getId().setUserId(activeUser.getId());
        activeUser.addUserRole(ur);
    }

    // =========================================================================
    // Flow A: Phone Verification
    // =========================================================================
    @Nested
    @DisplayName("sendPhoneVerificationOtp")
    class SendPhoneOtpTests {

        @Test
        @DisplayName("Thành công — lưu OTP và publish event")
        void sendPhoneOtp_success_savesOtpAndPublishesEvent() {
            SendPhoneOtpRequest req = new SendPhoneOtpRequest();
            req.setPhoneNumber("+84912345678");

            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
            when(userRepository.findByPhoneNumber("+84912345678")).thenReturn(Optional.empty());
            when(otpTokenRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(activeUser.getId()), eq(OtpType.PHONE_VERIFY), any())).thenReturn(0L);

            authService.sendPhoneVerificationOtp(activeUser.getId(), req);

            verify(otpTokenRepository).invalidateAllUnusedByUserIdAndType(activeUser.getId(), OtpType.PHONE_VERIFY);
            verify(otpTokenRepository).save(any(OtpToken.class));

            ArgumentCaptor<PhoneVerificationOtpMailEvent> captor = ArgumentCaptor.forClass(PhoneVerificationOtpMailEvent.class);
            verify(eventPublisher).publish(captor.capture());
            assertEquals("test@example.com", captor.getValue().toEmail());
            assertEquals(6, captor.getValue().otpCode().length());
        }

        @Test
        @DisplayName("Phone đã verified — throws PHONE_ALREADY_VERIFIED")
        void sendPhoneOtp_phoneAlreadyVerified_throws() {
            activeUser.setPhoneNumber("+84912345678");
            activeUser.setPhoneVerified(true);
            SendPhoneOtpRequest req = new SendPhoneOtpRequest();
            req.setPhoneNumber("+84912345678");

            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

            AppException ex = assertThrows(AppException.class,
                    () -> authService.sendPhoneVerificationOtp(activeUser.getId(), req));
            assertEquals(AuthErrorCode.PHONE_ALREADY_VERIFIED, ex.getExceptionCode());
        }

        @Test
        @DisplayName("Phone thuộc user khác — throws PHONE_ALREADY_TAKEN")
        void sendPhoneOtp_phoneTakenByOther_throws() {
            SendPhoneOtpRequest req = new SendPhoneOtpRequest();
            req.setPhoneNumber("+84912345678");

            User otherUser = new User();
            otherUser.setId(UUID.randomUUID());

            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
            when(userRepository.findByPhoneNumber("+84912345678")).thenReturn(Optional.of(otherUser));

            AppException ex = assertThrows(AppException.class,
                    () -> authService.sendPhoneVerificationOtp(activeUser.getId(), req));
            assertEquals(AuthErrorCode.PHONE_ALREADY_TAKEN, ex.getExceptionCode());
        }

        @Test
        @DisplayName("Rate limited — throws OTP_RATE_LIMITED")
        void sendPhoneOtp_rateLimited_throws() {
            SendPhoneOtpRequest req = new SendPhoneOtpRequest();
            req.setPhoneNumber("+84912345678");

            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
            when(userRepository.findByPhoneNumber("+84912345678")).thenReturn(Optional.empty());
            when(otpTokenRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(activeUser.getId()), eq(OtpType.PHONE_VERIFY), any())).thenReturn(5L);

            AppException ex = assertThrows(AppException.class,
                    () -> authService.sendPhoneVerificationOtp(activeUser.getId(), req));
            assertEquals(AuthErrorCode.OTP_RATE_LIMITED, ex.getExceptionCode());
        }
    }

    @Nested
    @DisplayName("verifyPhoneOtp")
    class VerifyPhoneOtpTests {

        @Test
        @DisplayName("Thành công — phone verified")
        void verifyPhoneOtp_success_setsPhoneVerified() {
            VerifyPhoneOtpRequest req = new VerifyPhoneOtpRequest();
            req.setPhoneNumber("+84912345678");
            req.setOtpCode("123456");

            OtpToken otp = OtpToken.of(activeUser.getId(), "123456", OtpType.PHONE_VERIFY, 5);

            when(otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(
                    activeUser.getId(), OtpType.PHONE_VERIFY)).thenReturn(Optional.of(otp));
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

            authService.verifyPhoneOtp(activeUser.getId(), req);

            assertTrue(otp.getUsed());
            assertEquals("+84912345678", activeUser.getPhoneNumber());
            assertTrue(activeUser.getPhoneVerified());
            verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("OTP hết hạn — throws OTP_EXPIRED")
        void verifyPhoneOtp_expired_throws() {
            VerifyPhoneOtpRequest req = new VerifyPhoneOtpRequest();
            req.setPhoneNumber("+84912345678");
            req.setOtpCode("123456");

            OtpToken otp = new OtpToken();
            otp.setCode("123456");
            otp.setUsed(false);
            otp.setExpiresAt(Instant.now().minusSeconds(60)); // expired

            when(otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(
                    activeUser.getId(), OtpType.PHONE_VERIFY)).thenReturn(Optional.of(otp));

            AppException ex = assertThrows(AppException.class,
                    () -> authService.verifyPhoneOtp(activeUser.getId(), req));
            assertEquals(AuthErrorCode.OTP_EXPIRED, ex.getExceptionCode());
        }

        @Test
        @DisplayName("OTP sai code — throws INVALID_OTP")
        void verifyPhoneOtp_wrongCode_throws() {
            VerifyPhoneOtpRequest req = new VerifyPhoneOtpRequest();
            req.setPhoneNumber("+84912345678");
            req.setOtpCode("000000");

            OtpToken otp = OtpToken.of(activeUser.getId(), "123456", OtpType.PHONE_VERIFY, 5);

            when(otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(
                    activeUser.getId(), OtpType.PHONE_VERIFY)).thenReturn(Optional.of(otp));

            AppException ex = assertThrows(AppException.class,
                    () -> authService.verifyPhoneOtp(activeUser.getId(), req));
            assertEquals(AuthErrorCode.INVALID_OTP, ex.getExceptionCode());
        }

        @Test
        @DisplayName("Không tìm thấy OTP — throws INVALID_OTP")
        void verifyPhoneOtp_notFound_throws() {
            VerifyPhoneOtpRequest req = new VerifyPhoneOtpRequest();
            req.setPhoneNumber("+84912345678");
            req.setOtpCode("123456");

            when(otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(
                    activeUser.getId(), OtpType.PHONE_VERIFY)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> authService.verifyPhoneOtp(activeUser.getId(), req));
            assertEquals(AuthErrorCode.INVALID_OTP, ex.getExceptionCode());
        }
    }

    // =========================================================================
    // Flow C: Login OTP
    // =========================================================================
    @Nested
    @DisplayName("sendLoginOtp")
    class SendLoginOtpTests {

        @Test
        @DisplayName("Thành công — lưu OTP và publish event")
        void sendLoginOtp_success_savesOtpAndPublishesEvent() {
            SendLoginOtpRequest req = new SendLoginOtpRequest();
            req.setEmail("test@example.com");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(otpTokenRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(activeUser.getId()), eq(OtpType.LOGIN_OTP), any())).thenReturn(0L);

            authService.sendLoginOtp(req);

            verify(otpTokenRepository).save(any(OtpToken.class));
            ArgumentCaptor<LoginOtpMailEvent> captor = ArgumentCaptor.forClass(LoginOtpMailEvent.class);
            verify(eventPublisher).publish(captor.capture());
            assertEquals("test@example.com", captor.getValue().toEmail());
        }

        @Test
        @DisplayName("Email không tồn tại — silent return (anti-enumeration)")
        void sendLoginOtp_unknownEmail_silentlyReturns() {
            SendLoginOtpRequest req = new SendLoginOtpRequest();
            req.setEmail("nonexist@example.com");

            when(userRepository.findByEmail("nonexist@example.com")).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> authService.sendLoginOtp(req));
            verify(otpTokenRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("Rate limited — throws OTP_RATE_LIMITED")
        void sendLoginOtp_rateLimited_throws() {
            SendLoginOtpRequest req = new SendLoginOtpRequest();
            req.setEmail("test@example.com");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(otpTokenRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(activeUser.getId()), eq(OtpType.LOGIN_OTP), any())).thenReturn(5L);

            assertThrows(AppException.class, () -> authService.sendLoginOtp(req));
        }
    }

    @Nested
    @DisplayName("loginWithOtp")
    class LoginWithOtpTests {

        @Test
        @DisplayName("Thành công — trả AuthResponse với tokens")
        void loginWithOtp_success_returnsAuthResponse() {
            LoginOtpRequest req = new LoginOtpRequest();
            req.setEmail("test@example.com");
            req.setOtpCode("654321");

            OtpToken otp = OtpToken.of(activeUser.getId(), "654321", OtpType.LOGIN_OTP, 5);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(
                    activeUser.getId(), OtpType.LOGIN_OTP)).thenReturn(Optional.of(otp));

            UserProfileResponse profile = new UserProfileResponse();
            when(userService.getProfile(activeUser.getId())).thenReturn(profile);
            when(jwtService.generateAccessToken(eq(activeUser.getId()), anyList())).thenReturn("access-token");
            when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
            when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);
            when(jwtService.getRefreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(86400));

            AuthResponse resp = authService.loginWithOtp(req, "127.0.0.1", "MobileApp/1.0");

            assertNotNull(resp);
            assertEquals("access-token", resp.getAccessToken());
            assertEquals("refresh-token", resp.getRefreshToken());
            assertTrue(otp.getUsed());
        }

        @Test
        @DisplayName("OTP sai — throws INVALID_OTP")
        void loginWithOtp_wrongCode_throws() {
            LoginOtpRequest req = new LoginOtpRequest();
            req.setEmail("test@example.com");
            req.setOtpCode("000000");

            OtpToken otp = OtpToken.of(activeUser.getId(), "654321", OtpType.LOGIN_OTP, 5);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(
                    activeUser.getId(), OtpType.LOGIN_OTP)).thenReturn(Optional.of(otp));

            AppException ex = assertThrows(AppException.class,
                    () -> authService.loginWithOtp(req, null, null));
            assertEquals(AuthErrorCode.INVALID_OTP, ex.getExceptionCode());
        }

        @Test
        @DisplayName("OTP hết hạn — throws OTP_EXPIRED")
        void loginWithOtp_expired_throws() {
            LoginOtpRequest req = new LoginOtpRequest();
            req.setEmail("test@example.com");
            req.setOtpCode("654321");

            OtpToken otp = new OtpToken();
            otp.setCode("654321");
            otp.setUsed(false);
            otp.setExpiresAt(Instant.now().minusSeconds(60));

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(
                    activeUser.getId(), OtpType.LOGIN_OTP)).thenReturn(Optional.of(otp));

            AppException ex = assertThrows(AppException.class,
                    () -> authService.loginWithOtp(req, null, null));
            assertEquals(AuthErrorCode.OTP_EXPIRED, ex.getExceptionCode());
        }

        @Test
        @DisplayName("Account disabled — throws ACCOUNT_DISABLED")
        void loginWithOtp_accountDisabled_throws() {
            activeUser.setStatus(UserStatuses.SUSPENDED);
            LoginOtpRequest req = new LoginOtpRequest();
            req.setEmail("test@example.com");
            req.setOtpCode("654321");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));

            AppException ex = assertThrows(AppException.class,
                    () -> authService.loginWithOtp(req, null, null));
            assertEquals(AuthErrorCode.ACCOUNT_DISABLED, ex.getExceptionCode());
        }

        @Test
        @DisplayName("Email chưa verified — throws EMAIL_NOT_VERIFIED")
        void loginWithOtp_emailNotVerified_throws() {
            activeUser.setEmailVerified(false);
            LoginOtpRequest req = new LoginOtpRequest();
            req.setEmail("test@example.com");
            req.setOtpCode("654321");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));

            AppException ex = assertThrows(AppException.class,
                    () -> authService.loginWithOtp(req, null, null));
            assertEquals(AuthErrorCode.EMAIL_NOT_VERIFIED, ex.getExceptionCode());
        }
    }
}
