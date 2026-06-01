package com.unishare.api.modules.booking.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.booking.dto.*;
import com.unishare.api.modules.booking.service.SessionReportService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Session Report Requests")
public class SessionReportController {

    private final SessionReportService sessionReportService;

    @Operation(summary = "Mentor tạo yêu cầu nộp báo cáo cho booking")
    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/api/v1/bookings/{bookingId}/report-requests")
    public ResponseEntity<ApiResponse<SessionReportRequestResponse>> createRequest(
                    @AuthenticationPrincipal CustomUserPrincipal principal,
                    @PathVariable UUID bookingId,
                    @Valid @RequestBody CreateReportRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.<SessionReportRequestResponse>build()
                        .withData(sessionReportService.createRequest(principal.getUserId(), bookingId, dto)));
    }

    @Operation(summary = "Lấy danh sách yêu cầu báo cáo theo booking")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
    @GetMapping("/api/v1/bookings/{bookingId}/report-requests")
    public ResponseEntity<ApiResponse<List<SessionReportRequestResponse>>> listForBooking(
                    @AuthenticationPrincipal CustomUserPrincipal principal,
                    @PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.<List<SessionReportRequestResponse>>build()
                        .withData(sessionReportService.listForBooking(bookingId, principal.getUserId())));
    }

    @Operation(summary = "Mentee nộp báo cáo")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
    @PostMapping("/api/v1/report-requests/{requestId}/submit")
    public ResponseEntity<ApiResponse<SessionReportRequestResponse>> submit(
                    @AuthenticationPrincipal CustomUserPrincipal principal,
                    @PathVariable UUID requestId,
                    @Valid @RequestBody SubmitReportDto dto) {
        return ResponseEntity.ok(ApiResponse.<SessionReportRequestResponse>build()
                        .withData(sessionReportService.submit(principal.getUserId(), requestId, dto)));
    }

    @Operation(summary = "Mentor duyệt báo cáo")
    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/api/v1/report-requests/{requestId}/review")
    public ResponseEntity<ApiResponse<SessionReportRequestResponse>> review(
                    @AuthenticationPrincipal CustomUserPrincipal principal,
                    @PathVariable UUID requestId,
                    @Valid @RequestBody ReviewReportDto dto) {
        return ResponseEntity.ok(ApiResponse.<SessionReportRequestResponse>build()
                        .withData(sessionReportService.review(principal.getUserId(), requestId, dto)));
    }

    @Operation(summary = "Mentee xem yêu cầu báo cáo của mình")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
    @GetMapping("/api/v1/report-requests/me/mentee")
    public ResponseEntity<ApiResponse<PageResponse<SessionReportRequestResponse>>> listForMentee(
                    @AuthenticationPrincipal CustomUserPrincipal principal,
                    @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<SessionReportRequestResponse>>build()
                        .withData(sessionReportService.listForMentee(principal.getUserId(), pageable)));
    }

    @Operation(summary = "Mentor xem yêu cầu báo cáo của mình")
    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/api/v1/report-requests/me/mentor")
    public ResponseEntity<ApiResponse<PageResponse<SessionReportRequestResponse>>> listForMentor(
                    @AuthenticationPrincipal CustomUserPrincipal principal,
                    @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<SessionReportRequestResponse>>build()
                        .withData(sessionReportService.listForMentor(principal.getUserId(), pageable)));
    }
}
