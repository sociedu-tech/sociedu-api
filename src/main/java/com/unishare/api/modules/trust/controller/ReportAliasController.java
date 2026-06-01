package com.unishare.api.modules.trust.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.trust.dto.*;
import com.unishare.api.modules.trust.service.TrustService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Trust alias — Reports")
@Deprecated
public class ReportAliasController {

    private final TrustService trustService;

    @Operation(summary = "Tạo báo cáo kiểm duyệt (Alias)")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ModerationReportResponse>> createReport(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateModerationReportRequest request) {
        return ResponseEntity.ok(ApiResponse.<ModerationReportResponse>build()
                .withData(trustService.createReport(principal.getUserId(), request)));
    }

    @Operation(summary = "Báo cáo của tôi (Alias)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<ModerationReportResponse>>> myReports(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<ModerationReportResponse>>build()
                .withData(trustService.myReports(principal.getUserId(), pageable)));
    }

    @Operation(summary = "Thêm bằng chứng cho báo cáo (Alias)")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
    @PostMapping("/{reportId}/evidences")
    public ResponseEntity<ApiResponse<ModerationReportResponse>> addEvidence(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID reportId,
            @Valid @RequestBody AddReportEvidenceRequest request) {
        return ResponseEntity.ok(ApiResponse.<ModerationReportResponse>build()
                .withData(trustService.addEvidence(principal.getUserId(), reportId, request)));
    }

    @Operation(summary = "Giải quyết báo cáo (Alias)")
    @PutMapping("/{reportId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModerationReportResponse>> resolveReport(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReportRequest request) {
        return ResponseEntity.ok(ApiResponse.<ModerationReportResponse>build()
                .withData(trustService.resolveReport(principal.getUserId(), reportId, request)));
    }
}
