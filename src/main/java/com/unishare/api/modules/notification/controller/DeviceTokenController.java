package com.unishare.api.modules.notification.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.notification.dto.RegisterDeviceRequest;
import com.unishare.api.modules.notification.dto.UnregisterDeviceRequest;
import com.unishare.api.modules.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Devices")
public class DeviceTokenController {

    private final NotificationService notificationService;

    @Operation(summary = "Đăng ký token thiết bị")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerDevice(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody RegisterDeviceRequest request) {
        notificationService.registerDevice(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.<Void>build().withMessage("Đăng ký thiết bị thành công"));
    }

    @Operation(summary = "Hủy đăng ký token thiết bị")
    @PostMapping("/unregister")
    public ResponseEntity<ApiResponse<Void>> unregisterDevice(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody UnregisterDeviceRequest request) {
        notificationService.unregisterDevice(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.<Void>build().withMessage("Hủy đăng ký thiết bị thành công"));
    }
}
