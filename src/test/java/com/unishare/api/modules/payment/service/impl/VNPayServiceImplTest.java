package com.unishare.api.modules.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.api.common.constants.PaymentTransactionStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.event.PaymentProcessedEvent;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.modules.payment.entity.PaymentTransaction;
import com.unishare.api.modules.payment.exception.PaymentErrorCode;
import com.unishare.api.modules.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VNPayServiceImplTest {

    private static final String HASH_SECRET = "secret-key";
    private static final String TMN_CODE = "TESTMERCHANT";

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private VNPayServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VNPayServiceImpl(paymentTransactionRepository, new ObjectMapper(), eventPublisher);
        ReflectionTestUtils.setField(service, "tmnCode", TMN_CODE);
        ReflectionTestUtils.setField(service, "hashSecret", HASH_SECRET);
        ReflectionTestUtils.setField(service, "vnpayUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(service, "returnUrl", "https://api.example.test/api/v1/payments/vnpay/return");
    }

    @Test
    void createPayment_ShouldPersistPendingTransactionAndReturnSignedUrl() {
        UUID orderId = UUID.randomUUID();
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> {
            PaymentTransaction txn = inv.getArgument(0);
            txn.setId(UUID.randomUUID());
            return txn;
        });

        var response = service.createPayment(orderId, new BigDecimal("150000.00"), "Unishare order", "127.0.0.1");

        assertEquals(orderId, response.getOrderId());
        assertEquals(PaymentTransactionStatuses.PENDING, response.getStatus());
        assertNotNull(response.getPaymentUrl());
        assertTrue(response.getPaymentUrl().contains("vnp_SecureHash="));
        verify(paymentTransactionRepository).save(argThat(txn ->
                orderId.equals(txn.getOrderId())
                        && new BigDecimal("150000.00").compareTo(txn.getAmount()) == 0
                        && PaymentTransactionStatuses.PENDING.equals(txn.getStatus())));
    }

    @Test
    void handleVNPayCallback_ShouldMarkTransactionSuccessAndPublishEvent() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String txnRef = orderId + "_123";
        PaymentTransaction txn = pendingTransaction(paymentId, orderId, txnRef, new BigDecimal("120000.00"));
        when(paymentTransactionRepository.findByProviderTransactionId(txnRef)).thenReturn(Optional.of(txn));

        boolean success = service.handleVNPayCallback(signedParams(txnRef, "12000000", "00"));

        assertTrue(success);
        assertEquals(PaymentTransactionStatuses.SUCCESS, txn.getStatus());
        assertNotNull(txn.getRawResponse());
        verify(paymentTransactionRepository).save(txn);
        verify(eventPublisher).publish(any(PaymentProcessedEvent.class));
    }

    @Test
    void handleVNPayCallback_ShouldReturnExistingResultForDuplicateCallback() {
        UUID orderId = UUID.randomUUID();
        String txnRef = orderId + "_123";
        PaymentTransaction txn = pendingTransaction(UUID.randomUUID(), orderId, txnRef, new BigDecimal("120000.00"));
        txn.setStatus(PaymentTransactionStatuses.SUCCESS);
        when(paymentTransactionRepository.findByProviderTransactionId(txnRef)).thenReturn(Optional.of(txn));

        boolean success = service.handleVNPayCallback(signedParams(txnRef, "12000000", "00"));

        assertTrue(success);
        verify(paymentTransactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void handleVNPayCallback_ShouldRejectMismatchedAmount() {
        UUID orderId = UUID.randomUUID();
        String txnRef = orderId + "_123";
        PaymentTransaction txn = pendingTransaction(UUID.randomUUID(), orderId, txnRef, new BigDecimal("120000.00"));
        when(paymentTransactionRepository.findByProviderTransactionId(txnRef)).thenReturn(Optional.of(txn));

        AppException ex = assertThrows(AppException.class,
                () -> service.handleVNPayCallback(signedParams(txnRef, "11900000", "00")));

        assertEquals(PaymentErrorCode.INVALID_CALLBACK, ex.getExceptionCode());
        verify(paymentTransactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    private static PaymentTransaction pendingTransaction(UUID paymentId, UUID orderId, String txnRef, BigDecimal amount) {
        PaymentTransaction txn = new PaymentTransaction();
        txn.setId(paymentId);
        txn.setOrderId(orderId);
        txn.setProviderTransactionId(txnRef);
        txn.setAmount(amount);
        txn.setStatus(PaymentTransactionStatuses.PENDING);
        return txn;
    }

    private static Map<String, String> signedParams(String txnRef, String amount, String responseCode) {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", amount);
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TmnCode", TMN_CODE);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_SecureHash", hmacSHA512(HASH_SECRET, buildRawData(params)));
        return params;
    }

    private static String buildRawData(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static String hmacSHA512(String key, String data) {
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
            throw new IllegalStateException(e);
        }
    }
}
