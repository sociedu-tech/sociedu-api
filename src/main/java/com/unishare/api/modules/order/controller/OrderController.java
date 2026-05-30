package com.unishare.api.modules.order.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.order.dto.CheckoutRequest;
import com.unishare.api.modules.order.dto.OrderResponse;
import com.unishare.api.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * Đặt mua: tạo đơn pending_payment + paymentUrl VNPay.
     */
    @Operation(summary = "Checkout — tạo đơn & URL thanh toán")
    @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).CREATE_PAYMENT)")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest http) {
        String ip = Optional.ofNullable(http.getHeader("X-Forwarded-For"))
                .map(s -> s.split(",")[0].trim())
                .orElse(http.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.<OrderResponse>build()
                .withData(orderService.checkout(principal.getUserId(), request, ip))
                .withMessage("Tạo đơn thành công — redirect tới paymentUrl để thanh toán"));
    }

    /**
     * Lấy danh sách đơn hàng của mình
     */
    @Operation(summary = "Danh sách đơn của tôi")
    @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).VIEW_PAYMENT)")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<OrderResponse>>build()
                .withData(orderService.getMyOrders(principal.getUserId(), pageable)));
    }

    /**
     * Chi tiết đơn hàng
     */
    @Operation(summary = "Chi tiết đơn theo id")
    @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).VIEW_PAYMENT)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<OrderResponse>build()
                .withData(orderService.getOrderById(id, principal.getUserId())));
    }
}
