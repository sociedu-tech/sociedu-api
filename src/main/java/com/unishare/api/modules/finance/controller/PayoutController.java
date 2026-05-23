package com.unishare.api.modules.finance.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.finance.dto.CreatePayoutRequest;
import com.unishare.api.modules.finance.dto.PayoutRequestResponse;
import com.unishare.api.modules.finance.dto.RevenueSummaryResponse;
import com.unishare.api.modules.finance.service.PayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mentors/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MENTOR')")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Mentor Finance & Payouts")
public class PayoutController {

    private final PayoutService payoutService;

    @Operation(summary = "Lấy thông tin tổng hợp doanh thu và số dư")
    @GetMapping("/revenue-summary")
    public ResponseEntity<ApiResponse<RevenueSummaryResponse>> getRevenueSummary(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.<RevenueSummaryResponse>build()
                .withData(payoutService.getRevenueSummary(principal.getUserId())));
    }

    @Operation(summary = "Yêu cầu rút tiền (payout)")
    @PostMapping("/payouts")
    public ResponseEntity<ApiResponse<PayoutRequestResponse>> createPayoutRequest(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreatePayoutRequest request) {
        return ResponseEntity.ok(ApiResponse.<PayoutRequestResponse>build()
                .withData(payoutService.createPayoutRequest(principal.getUserId(), request))
                .withMessage("Gửi yêu cầu rút tiền thành công"));
    }

    @Operation(summary = "Lịch sử yêu cầu rút tiền của tôi")
    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<Page<PayoutRequestResponse>>> getMyPayouts(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<PayoutRequestResponse>>build()
                .withData(payoutService.getMentorPayoutRequests(principal.getUserId(), pageable)));
    }

    @Operation(summary = "Chi tiết một yêu cầu rút tiền")
    @GetMapping("/payouts/{id}")
    public ResponseEntity<ApiResponse<PayoutRequestResponse>> getPayoutDetail(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.<PayoutRequestResponse>build()
                .withData(payoutService.getPayoutRequest(principal.getUserId(), id)));
    }
}
