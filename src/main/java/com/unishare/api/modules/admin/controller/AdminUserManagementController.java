package com.unishare.api.modules.admin.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.modules.admin.dto.AdminUserSummaryResponse;
import com.unishare.api.modules.admin.dto.UpdateUserStatusRequest;
import com.unishare.api.modules.admin.dto.UpdateUserRoleRequest;
import com.unishare.api.modules.admin.service.AdminUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Admin - Users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserManagementController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserSummaryResponse>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<AdminUserSummaryResponse>>build()
                .withData(adminUserService.listUsers(role, status, q, pageable)));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<AdminUserSummaryResponse>> updateUserRole(
            @PathVariable("id") UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.<AdminUserSummaryResponse>build()
                .withData(adminUserService.updateUserRole(userId, request.getRole())));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserSummaryResponse>> updateUserStatus(
            @PathVariable("id") UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.<AdminUserSummaryResponse>build()
                .withData(adminUserService.updateUserStatus(userId, request.getStatus())));
    }
}
