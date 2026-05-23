package com.unishare.api.modules.payment.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.dto.CommonErrorCode;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.modules.payment.dto.PaymentResponse;
import com.unishare.api.modules.payment.exception.PaymentErrorCode;
import com.unishare.api.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments — VNPay")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${vnpay.frontend-return-url}")
    private String frontendReturnUrl;

    /**
     * VNPay IPN (Instant Payment Notification) - gọi bởi VNPay server
     * Không yêu cầu JWT (VNPay server gọi trực tiếp)
     */
    @Operation(summary = "VNPay IPN")
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIPN(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN received: txnRef={}", params.get("vnp_TxnRef"));
        try {
            paymentService.handleVNPayCallback(params);
            return ResponseEntity.ok(vnpayResponse("00", "Confirm Success"));
        } catch (AppException e) {
            log.warn("VNPay IPN rejected: txnRef={}, type={}", params.get("vnp_TxnRef"), e.getExceptionCode().getType());
            return ResponseEntity.ok(vnpayResponse(toVNPayRspCode(e), e.getExceptionCode().getType()));
        } catch (Exception e) {
            log.error("VNPay IPN error", e);
            return ResponseEntity.ok(vnpayResponse("99", "Unknown error"));
        }
    }

    /**
     * VNPay Return URL - redirect user sau khi thanh toán
     * Trả thông tin cho frontend biết kết quả
     */
    @Operation(summary = "VNPay return URL")
    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> params) {
        log.info("VNPay Return: txnRef={}, responseCode={}", params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"));
        String txnRef = params.get("vnp_TxnRef");
        try {
            boolean success = paymentService.handleVNPayCallback(params);
            UUID orderId = parseOrderIdFromTxnRef(txnRef);
            PaymentResponse payment = paymentService.getPaymentByOrderId(orderId);
            return redirectToFrontend(orderId, payment.getStatus(), txnRef, success ? "00" : params.get("vnp_ResponseCode"));
        } catch (AppException e) {
            log.warn("VNPay Return rejected: txnRef={}, type={}", txnRef, e.getExceptionCode().getType());
            UUID orderId = tryParseOrderIdFromTxnRef(txnRef);
            return redirectToFrontend(orderId, "failed", txnRef, e.getExceptionCode().getType());
        } catch (Exception e) {
            log.error("VNPay Return error", e);
            UUID orderId = tryParseOrderIdFromTxnRef(txnRef);
            return redirectToFrontend(orderId, "failed", txnRef, "UNKNOWN_ERROR");
        }
    }

    /**
     * Lấy trạng thái payment theo orderId
     */
    @Operation(summary = "Payment theo orderId")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).VIEW_PAYMENT)")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>build()
                .withData(paymentService.getPaymentByOrderId(orderId)));
    }

    /** txnRef format: orderUuid_timestamp (see {@link com.unishare.api.modules.payment.service.impl.VNPayServiceImpl#createPayment}). */
    private static UUID parseOrderIdFromTxnRef(String txnRef) {
        if (txnRef == null || txnRef.isBlank()) {
            throw new AppException(CommonErrorCode.BAD_REQUEST, "Thiếu vnp_TxnRef");
        }
        int u = txnRef.indexOf('_');
        if (u <= 0) {
            throw new AppException(CommonErrorCode.BAD_REQUEST, "Định dạng vnp_TxnRef không hợp lệ");
        }
        try {
            return UUID.fromString(txnRef.substring(0, u));
        } catch (IllegalArgumentException e) {
            throw new AppException(CommonErrorCode.BAD_REQUEST, "Định dạng vnp_TxnRef không hợp lệ");
        }
    }

    private static UUID tryParseOrderIdFromTxnRef(String txnRef) {
        try {
            return parseOrderIdFromTxnRef(txnRef);
        } catch (AppException e) {
            return null;
        }
    }

    private ResponseEntity<Void> redirectToFrontend(UUID orderId, String status, String txnRef, String code) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontendReturnUrl)
                .queryParam("status", status);
        if (code != null && !code.isBlank()) {
            builder.queryParam("code", code);
        }
        if (orderId != null) {
            builder.queryParam("orderId", orderId);
        }
        if (txnRef != null && !txnRef.isBlank()) {
            builder.queryParam("transactionRef", txnRef);
        }
        URI location = builder.build().encode().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    private static Map<String, String> vnpayResponse(String rspCode, String message) {
        return Map.of("RspCode", rspCode, "Message", message);
    }

    private static String toVNPayRspCode(AppException e) {
        if (e.getExceptionCode() == PaymentErrorCode.INVALID_SIGNATURE) {
            return "97";
        }
        if (e.getExceptionCode() == PaymentErrorCode.PAYMENT_NOT_FOUND) {
            return "01";
        }
        if (e.getExceptionCode() == PaymentErrorCode.INVALID_CALLBACK) {
            return "04";
        }
        return "99";
    }
}
