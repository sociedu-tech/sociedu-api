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
@RequestMapping("/api/v1/trust")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Trust & moderation")
public class TrustController {

    private final TrustService trustService;

    @Operation(summary = "Tạo báo cáo kiểm duyệt")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
    @PostMapping("/reports")
    public ResponseEntity<ApiResponse<ModerationReportResponse>> createReport(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateModerationReportRequest request) {
        return ResponseEntity.ok(ApiResponse.<ModerationReportResponse>build()
                .withData(trustService.createReport(principal.getUserId(), request)));
    }

    @Operation(summary = "Báo cáo của tôi")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/reports/me")
    public ResponseEntity<ApiResponse<PageResponse<ModerationReportResponse>>> myReports(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<ModerationReportResponse>>build()
                .withData(trustService.myReports(principal.getUserId(), pageable)));
    }

    @Operation(summary = "Thêm bằng chứng cho báo cáo")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
    @PostMapping("/reports/{reportId}/evidences")
    public ResponseEntity<ApiResponse<ModerationReportResponse>> addEvidence(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID reportId,
            @Valid @RequestBody AddReportEvidenceRequest request) {
        return ResponseEntity.ok(ApiResponse.<ModerationReportResponse>build()
                .withData(trustService.addEvidence(principal.getUserId(), reportId, request)));
    }

    @Operation(summary = "Giải quyết báo cáo")
    @PutMapping("/reports/{reportId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModerationReportResponse>> resolveReport(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReportRequest request) {
        return ResponseEntity.ok(ApiResponse.<ModerationReportResponse>build()
                .withData(trustService.resolveReport(principal.getUserId(), reportId, request)));
    }

    @Operation(summary = "Tạo tranh chấp")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
    @PostMapping("/disputes")
    public ResponseEntity<ApiResponse<DisputeResponse>> createDispute(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateDisputeRequest request) {
        return ResponseEntity.ok(ApiResponse.<DisputeResponse>build()
                .withData(trustService.createDispute(principal.getUserId(), request)));
    }

    @Operation(summary = "Tranh chấp của tôi")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/disputes/me")
    public ResponseEntity<ApiResponse<PageResponse<DisputeResponse>>> myDisputes(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<DisputeResponse>>build()
                .withData(trustService.myDisputes(principal.getUserId(), pageable)));
    }

    @Operation(summary = "Giải quyết tranh chấp")
    @PutMapping("/disputes/{disputeId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DisputeResponse>> resolveDispute(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID disputeId,
            @Valid @RequestBody ResolveDisputeRequest request) {
        return ResponseEntity.ok(ApiResponse.<DisputeResponse>build()
                .withData(trustService.resolveDispute(principal.getUserId(), disputeId, request)));
    }
}
