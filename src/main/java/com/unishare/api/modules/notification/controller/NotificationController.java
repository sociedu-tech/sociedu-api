package com.unishare.api.modules.notification.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.notification.dto.NotificationResponse;
import com.unishare.api.modules.notification.dto.UnreadCountResponse;
import com.unishare.api.modules.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Lấy danh sách thông báo của tôi")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<NotificationResponse>>build()
                .withData(notificationService.getUserNotifications(principal.getUserId(), pageable)));
    }

    @Operation(summary = "Lấy số lượng thông báo chưa đọc")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.<UnreadCountResponse>build()
                .withData(notificationService.getUnreadCount(principal.getUserId())));
    }

    @Operation(summary = "Đánh dấu đọc tất cả thông báo")
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> readAll(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        notificationService.markAllRead(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.<Void>build().withMessage("Đã đọc tất cả thông báo"));
    }

    @Operation(summary = "Đánh dấu đọc một thông báo")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> readOne(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id) {
        notificationService.markRead(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.<Void>build().withMessage("Đã đọc thông báo"));
    }
}
