package com.unishare.api.modules.service.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.service.dto.request.CreateReportRequest;
import com.unishare.api.modules.service.dto.request.ReviewReportRequest;
import com.unishare.api.modules.service.dto.response.ProgressReportResponse;
import com.unishare.api.modules.service.service.ProgressReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/progress-reports")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Progress reports — Unified")
public class ProgressReportController {

    private final ProgressReportService reportService;

    @Operation(summary = "Nộp báo cáo tiến độ (Unified)")
    @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).CREATE_REPORT)")
    @PostMapping
    public ResponseEntity<ApiResponse<ProgressReportResponse>> submitReport(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateReportRequest request) {
        ProgressReportResponse response = reportService.createReport(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.<ProgressReportResponse>build()
                .withData(response)
                .withMessage("Nộp báo cáo thành công"));
    }

    @Operation(summary = "Danh sách báo cáo tiến độ của tôi (Unified)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<ProgressReportResponse>>> getMyReports(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(value = "role", required = false) String role,
            Pageable pageable) {
        boolean isMentor = principal.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_MENTOR".equals(auth.getAuthority()));
        boolean isMentee = principal.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_USER".equals(auth.getAuthority()) || "ROLE_BUYER".equals(auth.getAuthority()));
        
        Page<ProgressReportResponse> reports;
        if ("mentor".equalsIgnoreCase(role)) {
            reports = reportService.getMentorReports(principal.getUserId(), pageable);
        } else if ("mentee".equalsIgnoreCase(role)) {
            reports = reportService.getMenteeReports(principal.getUserId(), pageable);
        } else {
            if (isMentor && !isMentee) {
                reports = reportService.getMentorReports(principal.getUserId(), pageable);
            } else {
                reports = reportService.getMenteeReports(principal.getUserId(), pageable);
            }
        }
        
        return ResponseEntity.ok(ApiResponse.<Page<ProgressReportResponse>>build()
                .withData(reports)
                .withMessage("Success"));
    }

    @Operation(summary = "Xem chi tiết báo cáo tiến độ")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProgressReportResponse>> getReportById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id) {
        ProgressReportResponse response = reportService.getReportById(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.<ProgressReportResponse>build()
                .withData(response)
                .withMessage("Success"));
    }

    @Operation(summary = "Mentor phản hồi báo cáo tiến độ (Unified)")
    @PreAuthorize("hasRole('MENTOR')")
    @RequestMapping(value = "/{id}/mentor-feedback", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<ProgressReportResponse>> reviewReport(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ReviewReportRequest request) {
        ProgressReportResponse response = reportService.reviewReport(principal.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.<ProgressReportResponse>build()
                .withData(response)
                .withMessage("Phản hồi báo cáo thành công"));
    }
}
