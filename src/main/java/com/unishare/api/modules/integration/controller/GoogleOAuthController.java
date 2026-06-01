package com.unishare.api.modules.integration.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.AppUrlsProperties;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.googlemeet.oauth.GoogleOAuthTokenService;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/integrations/google/oauth")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.google.meet", name = "enabled", havingValue = "true")
@Tag(name = "Google OAuth")
public class GoogleOAuthController {

    private final GoogleOAuthTokenService oauthTokenService;
    private final AppUrlsProperties appUrls;

    @Operation(summary = "Lấy URL kết nối Google Calendar (mentor)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/authorize")
    public ResponseEntity<ApiResponse<Map<String, String>>> authorize(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(value = "returnUrl", required = false) String returnUrl) {
        String url = oauthTokenService.buildAuthorizationUrl(principal.getUserId(), returnUrl);
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>build()
                .withData(Map.of("authorizationUrl", url)));
    }

    @Operation(summary = "OAuth callback từ Google (public)")
    @GetMapping("/callback")
    public void callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response) throws IOException {
        String returnPath = oauthTokenService.resolveReturnPath(state);

        if (StringUtils.hasText(error)) {
            log.warn("Google OAuth denied or failed: error={} state={}", error, state);
            oauthTokenService.discardState(state);
            response.sendRedirect(frontendRedirect(returnPath, "google=error"));
            return;
        }

        try {
            returnPath = oauthTokenService.handleCallback(code, state);
            response.sendRedirect(frontendRedirect(returnPath, "google=connected"));
        } catch (Exception ex) {
            log.warn("Google OAuth callback failed state={}", state, ex);
            oauthTokenService.discardState(state);
            response.sendRedirect(frontendRedirect(returnPath, "google=error"));
        }
    }

    @Operation(summary = "Trạng thái kết nối Google Calendar (mentor)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> status(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        boolean connected = oauthTokenService.isAuthorized(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.<Map<String, Boolean>>build()
                .withData(Map.of("connected", connected)));
    }

    private String frontendRedirect(String returnPath, String queryParam) {
        String base = StringUtils.hasText(appUrls.frontendBase()) ? appUrls.frontendBase() : "http://localhost:3000";
        String path = StringUtils.hasText(returnPath) ? returnPath : "/dashboard/mentoring";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String separator = path.contains("?") ? "&" : "?";
        return base + path + separator + queryParam;
    }
}
