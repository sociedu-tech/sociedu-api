package com.unishare.api.modules.admin.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.admin.dto.AdminModerationReportResponse;
import com.unishare.api.modules.admin.dto.AdminResolveModerationRequest;
import com.unishare.api.modules.admin.service.AdminModerationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/moderation/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Admin - Moderation")
@PreAuthorize("hasRole('ADMIN')")
public class AdminModerationController {

    private final AdminModerationService adminModerationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminModerationReportResponse>>> list(
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<AdminModerationReportResponse>>build()
                .withData(adminModerationService.list(segment, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminModerationReportResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<AdminModerationReportResponse>build()
                .withData(adminModerationService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminModerationReportResponse>> resolve(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AdminResolveModerationRequest request) {
        return ResponseEntity.ok(ApiResponse.<AdminModerationReportResponse>build()
                .withData(adminModerationService.resolve(principal.getUserId(), id, request)));
    }
}
