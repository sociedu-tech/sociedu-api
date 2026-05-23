package com.unishare.api.modules.finance.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.finance.dto.AdminReviewPayoutRequest;
import com.unishare.api.modules.finance.dto.PayoutRequestResponse;
import com.unishare.api.modules.finance.service.PayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/admin/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Admin Payout Management")
public class AdminPayoutController {

    private final PayoutService payoutService;

    @Operation(summary = "Danh sách yêu cầu rút tiền (Admin)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PayoutRequestResponse>>> getPayoutRequests(
            @RequestParam(value = "status", required = false) String status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PayoutRequestResponse> page;
        if (status != null && !status.trim().isEmpty()) {
            page = payoutService.getPayoutRequestsByStatus(status, pageable);
        } else {
            page = payoutService.getAllPayoutRequests(pageable);
        }
        return ResponseEntity.ok(ApiResponse.<Page<PayoutRequestResponse>>build()
                .withData(page));
    }

    @Operation(summary = "Duyệt yêu cầu rút tiền")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PayoutRequestResponse>> approvePayout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.<PayoutRequestResponse>build()
                .withData(payoutService.approvePayoutRequest(principal.getUserId(), id))
                .withMessage("Duyệt yêu cầu rút tiền thành công"));
    }

    @Operation(summary = "Từ chối yêu cầu rút tiền")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<PayoutRequestResponse>> rejectPayout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id,
            @RequestBody AdminReviewPayoutRequest request) {
        return ResponseEntity.ok(ApiResponse.<PayoutRequestResponse>build()
                .withData(payoutService.rejectPayoutRequest(principal.getUserId(), id, request))
                .withMessage("Từ chối yêu cầu rút tiền thành công"));
    }

    @Operation(summary = "Đánh dấu đã chuyển khoản thành công")
    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<PayoutRequestResponse>> payPayout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id,
            @RequestBody AdminReviewPayoutRequest request) {
        return ResponseEntity.ok(ApiResponse.<PayoutRequestResponse>build()
                .withData(payoutService.markPaid(principal.getUserId(), id, request))
                .withMessage("Đánh dấu thanh toán thành công"));
    }

    @Operation(summary = "Đánh dấu chuyển khoản thất bại")
    @PostMapping("/{id}/fail")
    public ResponseEntity<ApiResponse<PayoutRequestResponse>> failPayout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id,
            @RequestBody AdminReviewPayoutRequest request) {
        return ResponseEntity.ok(ApiResponse.<PayoutRequestResponse>build()
                .withData(payoutService.markFailed(principal.getUserId(), id, request))
                .withMessage("Đánh dấu thanh toán thất bại"));
    }
}
