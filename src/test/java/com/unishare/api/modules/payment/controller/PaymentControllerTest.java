package com.unishare.api.modules.payment.controller;

import com.unishare.api.common.constants.PaymentTransactionStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.modules.payment.dto.PaymentResponse;
import com.unishare.api.modules.payment.exception.PaymentErrorCode;
import com.unishare.api.modules.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PaymentController controller = new PaymentController(paymentService);
        ReflectionTestUtils.setField(controller, "frontendReturnUrl", "https://web.example.test/payment-result");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void vnpayIPN_ShouldReturnVNPaySuccessJson_WhenCallbackIsAccepted() throws Exception {
        when(paymentService.handleVNPayCallback(anyMap())).thenReturn(true);

        mockMvc.perform(get("/api/v1/payments/vnpay/ipn")
                        .param("vnp_TxnRef", UUID.randomUUID() + "_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }

    @Test
    void vnpayIPN_ShouldReturnInvalidSignatureCode_WhenSignatureFails() throws Exception {
        when(paymentService.handleVNPayCallback(anyMap()))
                .thenThrow(new AppException(PaymentErrorCode.INVALID_SIGNATURE));

        mockMvc.perform(get("/api/v1/payments/vnpay/ipn")
                        .param("vnp_TxnRef", UUID.randomUUID() + "_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"))
                .andExpect(jsonPath("$.Message").value("INVALID_SIGNATURE"));
    }

    @Test
    void vnpayReturn_ShouldRedirectToFrontendPaymentResult() throws Exception {
        UUID orderId = UUID.randomUUID();
        String txnRef = orderId + "_123";
        PaymentResponse payment = new PaymentResponse();
        payment.setOrderId(orderId);
        payment.setTransactionRef(txnRef);
        payment.setAmount(new BigDecimal("120000.00"));
        payment.setStatus(PaymentTransactionStatuses.SUCCESS);

        when(paymentService.handleVNPayCallback(anyMap())).thenReturn(true);
        when(paymentService.getPaymentByOrderId(eq(orderId))).thenReturn(payment);

        mockMvc.perform(get("/api/v1/payments/vnpay/return")
                        .param("vnp_TxnRef", txnRef)
                        .param("vnp_ResponseCode", "00"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("https://web.example.test/payment-result")))
                .andExpect(header().string("Location", containsString("orderId=" + orderId)))
                .andExpect(header().string("Location", containsString("status=success")))
                .andExpect(header().string("Location", containsString("transactionRef=" + txnRef)));
    }
}
