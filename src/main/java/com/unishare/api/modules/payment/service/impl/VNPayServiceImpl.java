package com.unishare.api.modules.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.api.common.constants.PaymentProviders;
import com.unishare.api.common.constants.PaymentTransactionStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.event.PaymentProcessedEvent;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.modules.payment.dto.PaymentResponse;
import com.unishare.api.modules.payment.entity.PaymentTransaction;
import com.unishare.api.modules.payment.exception.PaymentErrorCode;
import com.unishare.api.modules.payment.repository.PaymentTransactionRepository;
import com.unishare.api.modules.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VNPayServiceImpl implements PaymentService {

    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher eventPublisher;

    public VNPayServiceImpl(
            PaymentTransactionRepository paymentTransactionRepository,
            ObjectMapper objectMapper,
            DomainEventPublisher eventPublisher) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.url}")
    private String vnpayUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Override
    @Transactional
    public PaymentResponse createPayment(UUID orderId, BigDecimal amount, String orderInfo, String ipAddress) {
        String txnRef = orderId + "_" + System.currentTimeMillis();

        PaymentTransaction txn = new PaymentTransaction();
        txn.setOrderId(orderId);
        txn.setProvider(PaymentProviders.VNPAY);
        txn.setProviderTransactionId(txnRef);
        txn.setAmount(amount);
        txn.setStatus(PaymentTransactionStatuses.PENDING);
        paymentTransactionRepository.save(txn);

        String paymentUrl = buildVNPayUrl(txnRef, amount, orderInfo, ipAddress);

        PaymentResponse response = new PaymentResponse();
        response.setId(txn.getId());
        response.setOrderId(orderId);
        response.setProvider(PaymentProviders.VNPAY);
        response.setTransactionRef(txnRef);
        response.setAmount(amount);
        response.setStatus(PaymentTransactionStatuses.PENDING);
        response.setCreatedAt(txn.getCreatedAt());
        response.setPaymentUrl(paymentUrl);
        return response;
    }

    @Override
    @Transactional
    public boolean handleVNPayCallback(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new AppException(PaymentErrorCode.INVALID_CALLBACK, "Thiếu dữ liệu callback VNPay");
        }
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            throw new AppException(PaymentErrorCode.INVALID_SIGNATURE);
        }
        Map<String, String> vnpParams = new TreeMap<>(params);
        vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        String calculatedHash = hmacSHA512(hashSecret, buildRawData(vnpParams));
        if (!calculatedHash.equalsIgnoreCase(receivedHash)) {
            log.warn("VNPay callback: invalid signature for txnRef={}", params.get("vnp_TxnRef"));
            throw new AppException(PaymentErrorCode.INVALID_SIGNATURE);
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        boolean success = "00".equals(responseCode);

        PaymentTransaction txn = paymentTransactionRepository.findByProviderTransactionId(txnRef)
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        validateCallbackMatchesTransaction(params, txn);

        if (!PaymentTransactionStatuses.PENDING.equals(txn.getStatus())) {
            log.info("VNPay callback: transaction {} already processed as {}", txnRef, txn.getStatus());
            return PaymentTransactionStatuses.SUCCESS.equals(txn.getStatus());
        }

        txn.setStatus(success ? PaymentTransactionStatuses.SUCCESS : PaymentTransactionStatuses.FAILED);
        try {
            txn.setRawResponse(objectMapper.writeValueAsString(params));
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize VNPay raw_response", e);
        }
        paymentTransactionRepository.save(txn);

        UUID orderId = txn.getOrderId();
        eventPublisher.publish(new PaymentProcessedEvent(
                orderId,
                success,
                PaymentProviders.VNPAY,
                txn.getProviderTransactionId(),
                txn.getId()));

        log.info("VNPay payment {} for orderId={}: {}", txnRef, orderId, success ? "SUCCESS" : "FAILED");
        return success;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOrderPaid(UUID orderId) {
        return paymentTransactionRepository.findByOrderId(orderId).stream()
                .anyMatch(t -> PaymentTransactionStatuses.SUCCESS.equals(t.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        PaymentTransaction txn = paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .findFirst()
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        return toResponse(txn);
    }

    private String buildVNPayUrl(String txnRef, BigDecimal amount, String orderInfo, String ipAddress) {
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode);
        vnpParams.put("vnp_Amount", toVNPayAmount(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", ipAddress != null ? ipAddress : "127.0.0.1");
        vnpParams.put("vnp_CreateDate", LocalDateTime.now(VNPAY_ZONE).format(VNPAY_DATE_FORMAT));

        String rawData = buildRawData(vnpParams);
        String secureHash = hmacSHA512(hashSecret, rawData);
        return vnpayUrl + "?" + rawData + "&vnp_SecureHash=" + secureHash;
    }

    private String buildRawData(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private void validateCallbackMatchesTransaction(Map<String, String> params, PaymentTransaction txn) {
        String callbackTmnCode = params.get("vnp_TmnCode");
        if (callbackTmnCode == null || !callbackTmnCode.equals(tmnCode)) {
            throw new AppException(PaymentErrorCode.INVALID_CALLBACK, "Sai mã merchant VNPay");
        }

        String callbackAmount = params.get("vnp_Amount");
        String expectedAmount = toVNPayAmount(txn.getAmount());
        if (callbackAmount == null || !callbackAmount.equals(expectedAmount)) {
            throw new AppException(PaymentErrorCode.INVALID_CALLBACK, "Số tiền callback VNPay không khớp");
        }
    }

    private String toVNPayAmount(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .toPlainString();
    }

    private PaymentResponse toResponse(PaymentTransaction txn) {
        PaymentResponse response = new PaymentResponse();
        response.setId(txn.getId());
        response.setOrderId(txn.getOrderId());
        response.setProvider(txn.getProvider());
        response.setTransactionRef(txn.getProviderTransactionId());
        response.setAmount(txn.getAmount());
        response.setStatus(txn.getStatus());
        response.setCreatedAt(txn.getCreatedAt());
        return response;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Cannot compute HMAC-SHA512", e);
            throw new AppException(PaymentErrorCode.SIGNATURE_COMPUTATION_FAILED, "Không thể ký giao dịch");
        }
    }
}
