package com.unishare.api.infrastructure.googlemeet.oauth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.infrastructure.googlemeet.GoogleMeetProperties;
import com.unishare.api.infrastructure.googlemeet.exception.GoogleMeetErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.google.meet", name = "enabled", havingValue = "true")
public class GoogleOAuthTokenService {

    private static final List<String> SCOPES = List.of(CalendarScopes.CALENDAR);

    private static final String DEFAULT_RETURN_PATH = "/dashboard/mentoring";

    private final GoogleMeetProperties properties;
    private final Map<String, Instant> pendingStates = new ConcurrentHashMap<>();
    private final Map<String, String> pendingReturnPaths = new ConcurrentHashMap<>();

    private GoogleAuthorizationCodeFlow flow;

    @PostConstruct
    void init() throws GeneralSecurityException, IOException {
        if (!StringUtils.hasText(properties.getOauthClientPath())) {
            throw new IllegalStateException("app.google.meet.oauth-client-path is required when Google Meet is enabled");
        }
        if (!StringUtils.hasText(properties.getOauthRedirectUri())) {
            throw new IllegalStateException("app.google.meet.oauth-redirect-uri is required when Google Meet is enabled");
        }

        File tokenDir = new File(properties.getTokenStorePath());
        if (!tokenDir.exists() && !tokenDir.mkdirs()) {
            log.warn("Could not create Google OAuth token directory: {}", tokenDir.getAbsolutePath());
        }

        GoogleClientSecrets clientSecrets;
        try (FileReader reader = new FileReader(properties.getOauthClientPath())) {
            clientSecrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), reader);
        }

        flow = new GoogleAuthorizationCodeFlow.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        clientSecrets,
                        SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(tokenDir))
                .setAccessType("offline")
                .build();

        log.info(
                "Google OAuth client loaded redirectUri={} tokenStore={}",
                properties.getOauthRedirectUri(),
                tokenDir.getAbsolutePath());
    }

    public String buildAuthorizationUrl(UUID mentorUserId, String returnUrl) {
        String state = mentorUserId + ":" + UUID.randomUUID();
        pendingStates.put(state, Instant.now().plus(15, ChronoUnit.MINUTES));
        pendingReturnPaths.put(state, sanitizeReturnUrl(returnUrl));
        GoogleAuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl()
                .setRedirectUri(properties.getOauthRedirectUri())
                .setState(state)
                .setAccessType("offline");
        // Google OAuth 2.0: dùng prompt=consent (approval_prompt đã bị loại bỏ)
        authorizationUrl.set("prompt", "consent");
        return authorizationUrl.build();
    }

    public String handleCallback(String code, String state) throws IOException {
        validateState(state);
        String returnPath = consumeReturnPath(state);
        pendingStates.remove(state);
        UUID mentorUserId = parseMentorIdFromState(state);

        TokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(properties.getOauthRedirectUri())
                .execute();
        flow.createAndStoreCredential(tokenResponse, credentialKey(mentorUserId));

        log.info("Google Calendar connected for mentorUserId={}", mentorUserId);
        return returnPath;
    }

    public String resolveReturnPath(String state) {
        if (!StringUtils.hasText(state)) {
            return DEFAULT_RETURN_PATH;
        }
        String path = pendingReturnPaths.get(state);
        return path != null ? path : DEFAULT_RETURN_PATH;
    }

    public void discardState(String state) {
        if (!StringUtils.hasText(state)) {
            return;
        }
        pendingStates.remove(state);
        pendingReturnPaths.remove(state);
    }

    public boolean isAuthorized(UUID mentorUserId) {
        try {
            Credential credential = flow.loadCredential(credentialKey(mentorUserId));
            return credential != null && (credential.getRefreshToken() != null || credential.getAccessToken() != null);
        } catch (IOException ex) {
            log.warn("Failed to check Google OAuth status for mentor {}", mentorUserId, ex);
            return false;
        }
    }

    public Credential requireCredential(UUID mentorUserId) throws IOException {
        Credential credential = flow.loadCredential(credentialKey(mentorUserId));
        if (credential == null) {
            throw new AppException(
                    GoogleMeetErrorCode.NOT_AUTHORIZED,
                    "Mentor chưa kết nối Google Calendar. Gọi GET /api/v1/integrations/google/oauth/authorize để liên kết tài khoản.");
        }
        return credential;
    }

    private void validateState(String state) {
        if (!StringUtils.hasText(state)) {
            throw new AppException(GoogleMeetErrorCode.NOT_AUTHORIZED, "OAuth state không hợp lệ.");
        }
        Instant expires = pendingStates.get(state);
        if (expires == null || expires.isBefore(Instant.now())) {
            throw new AppException(GoogleMeetErrorCode.NOT_AUTHORIZED, "OAuth state hết hạn hoặc không hợp lệ.");
        }
    }

    private static UUID parseMentorIdFromState(String state) {
        try {
            return UUID.fromString(state.split(":")[0]);
        } catch (Exception ex) {
            throw new AppException(GoogleMeetErrorCode.NOT_AUTHORIZED, "OAuth state không hợp lệ.");
        }
    }

    private static String credentialKey(UUID mentorUserId) {
        return mentorUserId.toString();
    }

    private String consumeReturnPath(String state) {
        String path = pendingReturnPaths.remove(state);
        return sanitizeReturnUrl(path);
    }

    private static String sanitizeReturnUrl(String returnUrl) {
        if (!StringUtils.hasText(returnUrl)) {
            return DEFAULT_RETURN_PATH;
        }
        String trimmed = returnUrl.trim();
        if (!trimmed.startsWith("/dashboard") || trimmed.contains("://")) {
            return DEFAULT_RETURN_PATH;
        }
        return trimmed;
    }
}
