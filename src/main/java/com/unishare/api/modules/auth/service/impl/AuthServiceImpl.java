package com.unishare.api.modules.auth.service.impl;

import com.unishare.api.common.constants.Roles;
import com.unishare.api.common.constants.UserStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.common.event.EmailVerificationMailEvent;
import com.unishare.api.common.event.LoginOtpMailEvent;
import com.unishare.api.common.event.PasswordResetMailEvent;
import com.unishare.api.common.event.PhoneVerificationOtpMailEvent;
import com.unishare.api.modules.auth.dto.request.*;
import com.unishare.api.modules.auth.dto.response.AuthResponse;
import com.unishare.api.modules.auth.dto.response.MeResponse;
import com.unishare.api.modules.auth.dto.response.SessionResponse;
import com.unishare.api.modules.auth.exception.AuthErrorCode;
import com.unishare.api.infrastructure.security.JwtService;
import com.unishare.api.modules.auth.entity.*;
import com.unishare.api.modules.auth.repository.*;
import com.unishare.api.modules.auth.service.AuthService;
import com.unishare.api.modules.user.dto.UserProfileResponse;
import com.unishare.api.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int OTP_TTL_MINUTES = 10;
    private static final int PHONE_OTP_TTL_MINUTES = 5;
    private static final int LOGIN_OTP_TTL_MINUTES = 5;
    private static final int OTP_NUMERIC_LENGTH = 6;
    private static final int MAX_OTP_PER_HOUR = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    @Value("${app.auth.email-verification-page-url}")
    private String emailVerificationPageUrl;

    @Value("${app.auth.password-reset-page-url}")
    private String passwordResetPageUrl;

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final DomainEventPublisher eventPublisher;
    private final UserService userService;

    // ------------------------------------------------------------------ register
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(AuthErrorCode.EMAIL_ALREADY_EXISTS,
                    "Email đã được sử dụng: " + request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setEmailVerified(false);
        user.setStatus(UserStatuses.PENDING);

        UserCredential credential = new UserCredential();
        credential.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCredential(credential);

        Role buyerRole = roleRepository.findByName(Roles.USER)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, "Role USER not found"));

        UserRole userRole = new UserRole();
        userRole.setRole(buyerRole);
        userRole.getId().setRoleId(buyerRole.getId());
        user.addUserRole(userRole);

        user = userRepository.save(user);

        userService.createProfileForNewUser(
                user.getId(),
                request.getFirstName(),
                request.getLastName());

        sendVerificationLink(user);
        log.info("[Auth] Registered user: {}", user.getEmail());

        return buildRegisterResponse(user, userService.getProfile(user.getId()));
    }

    // ------------------------------------------------------------------ login
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(AuthErrorCode.INVALID_CREDENTIALS,
                        "Email hoặc mật khẩu không đúng"));

        UserCredential credential = userCredentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(AuthErrorCode.INVALID_CREDENTIALS,
                        "Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            log.warn("[Auth] Invalid password attempt for: {}", request.getEmail());
            throw new AppException(AuthErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AppException(AuthErrorCode.EMAIL_NOT_VERIFIED,
                    "Vui lòng xác minh email trước khi đăng nhập");
        }

        if (!UserStatuses.ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new AppException(AuthErrorCode.ACCOUNT_DISABLED, "Tài khoản đã bị vô hiệu hóa");
        }

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        UserProfileResponse profile = userService.getProfile(user.getId());

        log.info("[Auth] User logged in: {}", user.getEmail());
        return buildAuthResponse(user, roles, profile, ipAddress, userAgent);
    }

    // ------------------------------------------------------------------
    // refreshToken
    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new AppException(AuthErrorCode.INVALID_TOKEN, "Refresh token không hợp lệ"));

        if (stored.isExpired()) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new AppException(AuthErrorCode.TOKEN_EXPIRED, "Refresh token đã hết hạn");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, "Người dùng không tồn tại"));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new AppException(AuthErrorCode.EMAIL_NOT_VERIFIED,
                    "Vui lòng xác minh email trước khi sử dụng phiên đăng nhập");
        }

        if (!UserStatuses.ACTIVE.equalsIgnoreCase(user.getStatus())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new AppException(AuthErrorCode.ACCOUNT_DISABLED, "Tài khoản đã bị vô hiệu hóa");
        }

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        UserProfileResponse profile = userService.getProfile(user.getId());

        stored.setLastUsedAt(Instant.now());
        if (ipAddress != null && !ipAddress.isBlank()) {
            stored.setIpAddress(ipAddress.trim());
        }
        if (userAgent != null && !userAgent.isBlank()) {
            stored.setUserAgent(userAgent.trim());
            stored.setDeviceInfo(toDeviceInfo(userAgent));
        }
        refreshTokenRepository.save(stored);

        String newAccessToken = jwtService.generateAccessToken(user.getId(), roles);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(stored.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .roles(roles)
                .build();
    }

    // ------------------------------------------------------------------ logout
    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    log.info("[Auth] User {} logged out", rt.getUserId());
                });
    }

    // ------------------------------------------------------------------
    // sendVerificationEmail
    @Override
    @Transactional
    public void sendVerificationEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                log.debug("[Auth] Resend verification skipped: already verified userId={}", user.getId());
                return;
            }
            sendVerificationLink(user);
        });
    }

    private void sendVerificationLink(User user) {
        String token = generateSecureToken();
        OtpToken otpToken = OtpToken.of(user.getId(), token, OtpType.EMAIL_VERIFY, OTP_TTL_MINUTES);
        otpTokenRepository.save(otpToken);
        String link = appendTokenQuery(emailVerificationPageUrl, token);
        eventPublisher.publish(new EmailVerificationMailEvent(user.getEmail(), link));
        log.info("[Auth] Verification email queued for userId={}", user.getId());
    }

    // ------------------------------------------------------------------
    // verifyEmail
    @Override
    @Transactional
    public AuthResponse verifyEmail(String token) {
        OtpToken otp = otpTokenRepository
                .findByCodeAndTypeAndUsedFalse(token, OtpType.EMAIL_VERIFY)
                .orElseThrow(() -> new AppException(AuthErrorCode.INVALID_OTP,
                        "Liên kết xác minh không hợp lệ hoặc đã được sử dụng"));

        if (otp.isExpired()) {
            throw new AppException(AuthErrorCode.OTP_EXPIRED, "Liên kết xác minh đã hết hạn");
        }

        User user = userRepository.findById(otp.getUserId())
                .orElseThrow(() -> new AppException(AuthErrorCode.INVALID_OTP, "Liên kết xác minh không hợp lệ"));

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();
        UserProfileResponse profile = userService.getProfile(user.getId());

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            otp.setUsed(true);
            otpTokenRepository.save(otp);
            return buildAuthResponse(user, roles, profile, null, null);
        }

        otp.setUsed(true);
        otpTokenRepository.save(otp);

        user.setEmailVerified(true);
        user.setStatus(UserStatuses.ACTIVE);
        userRepository.save(user);
        log.info("[Auth] Email verified for userId={}", user.getId());
        return buildAuthResponse(user, roles, profile, null, null);
    }

    // ------------------------------------------------------------------
    // forgotPassword
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Silently ignore unknown emails to prevent user enumeration
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = generateSecureToken();
            OtpToken otpToken = OtpToken.of(user.getId(), token, OtpType.PASSWORD_RESET, OTP_TTL_MINUTES);
            otpTokenRepository.save(otpToken);
            String link = appendTokenQuery(passwordResetPageUrl, token);
            eventPublisher.publish(new PasswordResetMailEvent(user.getEmail(), link));
            log.info("[Auth] Password reset email queued for userId={}", user.getId());
        });
    }

    // ------------------------------------------------------------------
    // resetPassword
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        OtpToken otp = otpTokenRepository
                .findByCodeAndTypeAndUsedFalse(request.getToken(), OtpType.PASSWORD_RESET)
                .orElseThrow(() -> new AppException(AuthErrorCode.INVALID_OTP,
                        "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã được sử dụng"));

        if (otp.isExpired()) {
            throw new AppException(AuthErrorCode.OTP_EXPIRED, "Liên kết đặt lại mật khẩu đã hết hạn");
        }

        User user = userRepository.findById(otp.getUserId())
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, "Người dùng không tồn tại"));

        otp.setUsed(true);
        otpTokenRepository.save(otp);

        userCredentialRepository.findByUserId(user.getId()).ifPresent(credential -> {
            credential.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            credential.setUpdatedAt(Instant.now());
            userCredentialRepository.save(credential);
        });

        // Revoke all refresh tokens so other sessions are invalidated
        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("[Auth] Password reset for userId={}", user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, "Người dùng không tồn tại"));
        UserProfileResponse profile = userService.getProfile(userId);
        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();
        return MeResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .emailVerified(user.getEmailVerified())
                .status(user.getStatus())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .fullName(displayNameFromProfile(profile))
                .headline(profile.getHeadline())
                .avatarUrl(null)
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request, String currentRefreshToken) {
        UserCredential credential = userCredentialRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, "Người dùng không tồn tại"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), credential.getPasswordHash())) {
            throw new AppException(AuthErrorCode.INVALID_CURRENT_PASSWORD, "Mật khẩu hiện tại không chính xác");
        }

        credential.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        credential.setUpdatedAt(Instant.now());
        userCredentialRepository.save(credential);

        Instant now = Instant.now();
        UUID currentSessionId = resolveCurrentSessionId(userId, currentRefreshToken);
        List<RefreshToken> sessions = refreshTokenRepository.findActiveSessionsByUserId(userId, now);
        for (RefreshToken session : sessions) {
            if (currentSessionId != null && currentSessionId.equals(session.getId())) {
                continue;
            }
            session.setRevoked(true);
        }
        refreshTokenRepository.saveAll(sessions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(UUID userId, String currentRefreshToken) {
        UUID currentSessionId = resolveCurrentSessionId(userId, currentRefreshToken);
        return refreshTokenRepository.findActiveSessionsByUserId(userId, Instant.now()).stream()
                .map(rt -> SessionResponse.builder()
                        .id(rt.getId())
                        .deviceInfo(rt.getDeviceInfo())
                        .ipAddress(rt.getIpAddress())
                        .userAgent(rt.getUserAgent())
                        .createdAt(rt.getCreatedAt())
                        .lastUsedAt(rt.getLastUsedAt())
                        .expiresAt(rt.getExpiresAt())
                        .current(currentSessionId != null && currentSessionId.equals(rt.getId()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        RefreshToken session = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(AuthErrorCode.SESSION_NOT_FOUND, "Phiên đăng nhập không tồn tại"));
        if (!userId.equals(session.getUserId())) {
            throw new AppException(AuthErrorCode.ACCESS_DENIED, "Bạn không có quyền thu hồi phiên này");
        }
        session.setRevoked(true);
        refreshTokenRepository.save(session);
    }

    @Override
    @Transactional
    public void revokeAllSessions(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // ------------------------------------------------------------------ Flow A: Phone Verification
    @Override
    @Transactional
    public void sendPhoneVerificationOtp(UUID userId, SendPhoneOtpRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, "Người dùng không tồn tại"));

        if (Boolean.TRUE.equals(user.getPhoneVerified())
                && request.getPhoneNumber().equals(user.getPhoneNumber())) {
            throw new AppException(AuthErrorCode.PHONE_ALREADY_VERIFIED, "Số điện thoại này đã được xác thực");
        }

        // Kiểm tra phone đã thuộc user khác chưa
        userRepository.findByPhoneNumber(request.getPhoneNumber()).ifPresent(other -> {
            if (!other.getId().equals(userId)) {
                throw new AppException(AuthErrorCode.PHONE_ALREADY_TAKEN, "Số điện thoại đã được sử dụng bởi tài khoản khác");
            }
        });

        checkOtpRateLimit(userId, OtpType.PHONE_VERIFY);

        otpTokenRepository.invalidateAllUnusedByUserIdAndType(userId, OtpType.PHONE_VERIFY);
        String code = generateNumericOtp();
        OtpToken otp = OtpToken.of(userId, code, OtpType.PHONE_VERIFY, PHONE_OTP_TTL_MINUTES);
        otpTokenRepository.save(otp);

        eventPublisher.publish(new PhoneVerificationOtpMailEvent(user.getEmail(), code));
        log.info("[Auth] Phone verification OTP sent to email for userId={}", userId);
    }

    @Override
    @Transactional
    public void verifyPhoneOtp(UUID userId, VerifyPhoneOtpRequest request) {
        OtpToken otp = otpTokenRepository
                .findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(userId, OtpType.PHONE_VERIFY)
                .orElseThrow(() -> new AppException(AuthErrorCode.INVALID_OTP, "Mã OTP không hợp lệ hoặc đã được sử dụng"));

        if (otp.isExpired()) {
            throw new AppException(AuthErrorCode.OTP_EXPIRED, "Mã OTP đã hết hạn");
        }
        if (!otp.getCode().equals(request.getOtpCode())) {
            throw new AppException(AuthErrorCode.INVALID_OTP, "Mã OTP không chính xác");
        }

        otp.setUsed(true);
        otpTokenRepository.save(otp);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, "Người dùng không tồn tại"));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPhoneVerified(true);
        userRepository.save(user);
        log.info("[Auth] Phone verified for userId={}, phone={}", userId, maskPhone(request.getPhoneNumber()));
    }

    // ------------------------------------------------------------------ Flow C: Login OTP
    @Override
    @Transactional
    public void sendLoginOtp(SendLoginOtpRequest request) {
        // Anti-enumeration: silent return nếu email không tồn tại
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                log.debug("[Auth] Login OTP skipped: email not verified userId={}", user.getId());
                return;
            }
            if (!UserStatuses.ACTIVE.equalsIgnoreCase(user.getStatus())) {
                log.debug("[Auth] Login OTP skipped: account not active userId={}", user.getId());
                return;
            }

            checkOtpRateLimit(user.getId(), OtpType.LOGIN_OTP);

            otpTokenRepository.invalidateAllUnusedByUserIdAndType(user.getId(), OtpType.LOGIN_OTP);
            String code = generateNumericOtp();
            OtpToken otp = OtpToken.of(user.getId(), code, OtpType.LOGIN_OTP, LOGIN_OTP_TTL_MINUTES);
            otpTokenRepository.save(otp);

            eventPublisher.publish(new LoginOtpMailEvent(user.getEmail(), code));
            log.info("[Auth] Login OTP sent to email for userId={}", user.getId());
        });
    }

    @Override
    @Transactional
    public AuthResponse loginWithOtp(LoginOtpRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(AuthErrorCode.INVALID_OTP, "Email hoặc mã OTP không đúng"));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AppException(AuthErrorCode.EMAIL_NOT_VERIFIED, "Vui lòng xác minh email trước khi đăng nhập");
        }
        if (!UserStatuses.ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new AppException(AuthErrorCode.ACCOUNT_DISABLED, "Tài khoản đã bị vô hiệu hóa");
        }

        OtpToken otp = otpTokenRepository
                .findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(user.getId(), OtpType.LOGIN_OTP)
                .orElseThrow(() -> new AppException(AuthErrorCode.INVALID_OTP, "Email hoặc mã OTP không đúng"));

        if (otp.isExpired()) {
            throw new AppException(AuthErrorCode.OTP_EXPIRED, "Mã OTP đã hết hạn");
        }
        if (!otp.getCode().equals(request.getOtpCode())) {
            throw new AppException(AuthErrorCode.INVALID_OTP, "Email hoặc mã OTP không đúng");
        }

        otp.setUsed(true);
        otpTokenRepository.save(otp);

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();
        UserProfileResponse profile = userService.getProfile(user.getId());

        log.info("[Auth] User logged in via OTP: {}", user.getEmail());
        return buildAuthResponse(user, roles, profile, ipAddress, userAgent);
    }

    // ------------------------------------------------------------------ helpers

    private void checkOtpRateLimit(UUID userId, OtpType type) {
        long count = otpTokenRepository.countByUserIdAndTypeAndCreatedAtAfter(
                userId, type, Instant.now().minusSeconds(3600));
        if (count >= MAX_OTP_PER_HOUR) {
            throw new AppException(AuthErrorCode.OTP_RATE_LIMITED,
                    "Bạn đã gửi quá nhiều mã OTP. Vui lòng thử lại sau.");
        }
    }

    private String generateNumericOtp() {
        StringBuilder sb = new StringBuilder(OTP_NUMERIC_LENGTH);
        for (int i = 0; i < OTP_NUMERIC_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, phone.length() - 4) + "****";
    }
    private AuthResponse buildAuthResponse(User user, List<String> roles, UserProfileResponse profile,
            String ipAddress, String userAgent) {
        String accessToken = jwtService.generateAccessToken(user.getId(), roles);
        String rawRefreshToken = jwtService.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.of(
                user.getId(),
                rawRefreshToken,
                jwtService.getRefreshTokenExpiry(),
                ipAddress,
                userAgent,
                toDeviceInfo(userAgent));
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .roles(roles)
                .build();
    }

    private AuthResponse buildRegisterResponse(User user, UserProfileResponse profile) {
        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .tokenType("Bearer")
                .expiresIn(null)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .roles(roles)
                .build();
    }

    private static String displayNameFromProfile(UserProfileResponse p) {
        String a = p.getFirstName() != null ? p.getFirstName().trim() : "";
        String b = p.getLastName() != null ? p.getLastName().trim() : "";
        String s = (a + " " + b).trim();
        return s.isEmpty() ? "Người dùng" : s;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }

    private String appendTokenQuery(String pageUrl, String token) {
        String enc = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String sep = pageUrl.contains("?") ? "&" : "?";
        return pageUrl + sep + "token=" + enc;
    }

    private UUID resolveCurrentSessionId(UUID userId, String currentRefreshToken) {
        if (currentRefreshToken == null || currentRefreshToken.isBlank()) {
            return null;
        }
        return refreshTokenRepository.findByTokenAndRevokedFalse(currentRefreshToken.trim())
                .filter(rt -> userId.equals(rt.getUserId()))
                .map(RefreshToken::getId)
                .orElse(null);
    }

    private String toDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String compact = userAgent.replaceAll("\\s+", " ").trim();
        return compact.length() > 255 ? compact.substring(0, 255) : compact;
    }
}
