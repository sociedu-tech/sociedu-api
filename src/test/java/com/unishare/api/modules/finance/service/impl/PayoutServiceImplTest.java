package com.unishare.api.modules.finance.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.finance.dto.AdminReviewPayoutRequest;
import com.unishare.api.modules.finance.dto.CreatePayoutRequest;
import com.unishare.api.modules.finance.dto.PayoutRequestResponse;
import com.unishare.api.modules.finance.dto.RevenueSummaryResponse;
import com.unishare.api.modules.finance.entity.PayoutAuditLog;
import com.unishare.api.modules.finance.entity.PayoutRequest;
import com.unishare.api.modules.finance.exception.FinanceErrorCode;
import com.unishare.api.modules.finance.repository.PayoutAuditLogRepository;
import com.unishare.api.modules.finance.repository.PayoutRequestRepository;
import com.unishare.api.modules.mentor.entity.MentorProfile;
import com.unishare.api.modules.mentor.exception.MentorErrorCode;
import com.unishare.api.modules.mentor.repository.MentorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceImplTest {

    @Mock
    private PayoutRequestRepository payoutRequestRepository;

    @Mock
    private PayoutAuditLogRepository payoutAuditLogRepository;

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PayoutServiceImpl payoutService;

    private UUID mentorId;
    private MentorProfile mentorProfile;
    private PayoutRequest payoutRequest;

    @BeforeEach
    void setUp() {
        mentorId = UUID.randomUUID();
        mentorProfile = new MentorProfile();
        mentorProfile.setUserId(mentorId);

        payoutRequest = new PayoutRequest();
        payoutRequest.setId(UUID.randomUUID());
        payoutRequest.setMentorId(mentorId);
        payoutRequest.setGrossAmount(BigDecimal.valueOf(100000));
        payoutRequest.setPlatformFeeRate(BigDecimal.valueOf(10.00));
        payoutRequest.setNetAmount(BigDecimal.valueOf(90000));
        payoutRequest.setStatus("PENDING");
        payoutRequest.setBankName("VCB");
        payoutRequest.setAccountNumber("123456789");
        payoutRequest.setAccountHolder("Nguyen Van A");
    }

    @Test
    void createPayoutRequest_Success() {
        CreatePayoutRequest request = new CreatePayoutRequest();
        request.setAmount(BigDecimal.valueOf(100000));
        request.setBankName("VCB");
        request.setAccountNumber("123456789");
        request.setAccountHolder("Nguyen Van A");

        when(mentorProfileRepository.findAndLockByUserId(mentorId)).thenReturn(Optional.of(mentorProfile));
        when(bookingRepository.calculateTotalEarnedByMentor(mentorId)).thenReturn(BigDecimal.valueOf(1000000));
        when(payoutRequestRepository.calculateTotalWithdrawnByMentor(mentorId)).thenReturn(BigDecimal.valueOf(200000));
        when(payoutRequestRepository.calculateLockedBalanceByMentor(mentorId)).thenReturn(BigDecimal.valueOf(100000));

        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenReturn(payoutRequest);

        PayoutRequestResponse response = payoutService.createPayoutRequest(mentorId, request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals(BigDecimal.valueOf(100000), response.getGrossAmount());
        assertEquals("*******6789", response.getAccountNumber()); // Masked account number

        verify(payoutRequestRepository).save(any(PayoutRequest.class));
        verify(payoutAuditLogRepository).save(any(PayoutAuditLog.class));
    }

    @Test
    void createPayoutRequest_InsufficientBalance_ThrowsException() {
        CreatePayoutRequest request = new CreatePayoutRequest();
        request.setAmount(BigDecimal.valueOf(800000));
        request.setBankName("VCB");
        request.setAccountNumber("123456789");
        request.setAccountHolder("Nguyen Van A");

        when(mentorProfileRepository.findAndLockByUserId(mentorId)).thenReturn(Optional.of(mentorProfile));
        when(bookingRepository.calculateTotalEarnedByMentor(mentorId)).thenReturn(BigDecimal.valueOf(1000000));
        when(payoutRequestRepository.calculateTotalWithdrawnByMentor(mentorId)).thenReturn(BigDecimal.valueOf(200000));
        when(payoutRequestRepository.calculateLockedBalanceByMentor(mentorId)).thenReturn(BigDecimal.valueOf(100000));

        AppException ex = assertThrows(AppException.class, () ->
                payoutService.createPayoutRequest(mentorId, request));

        assertEquals(FinanceErrorCode.INSUFFICIENT_BALANCE.getCode(), ex.getExceptionCode().getCode());
    }

    @Test
    void getRevenueSummary_Success() {
        when(mentorProfileRepository.findById(mentorId)).thenReturn(Optional.of(mentorProfile));
        when(bookingRepository.calculateTotalEarnedByMentor(mentorId)).thenReturn(BigDecimal.valueOf(1000000));
        when(payoutRequestRepository.calculateTotalWithdrawnByMentor(mentorId)).thenReturn(BigDecimal.valueOf(200000));
        when(payoutRequestRepository.calculateLockedBalanceByMentor(mentorId)).thenReturn(BigDecimal.valueOf(100000));

        RevenueSummaryResponse summary = payoutService.getRevenueSummary(mentorId);

        assertNotNull(summary);
        assertEquals(BigDecimal.valueOf(1000000), summary.getTotalEarned());
        assertEquals(BigDecimal.valueOf(200000), summary.getTotalWithdrawn());
        assertEquals(BigDecimal.valueOf(100000), summary.getLockedBalance());
        assertEquals(BigDecimal.valueOf(700000), summary.getAvailableBalance());
    }

    @Test
    void markPaid_Success() {
        UUID adminId = UUID.randomUUID();
        AdminReviewPayoutRequest review = new AdminReviewPayoutRequest();
        review.setTransactionReference("REF123");

        when(payoutRequestRepository.findById(payoutRequest.getId())).thenReturn(Optional.of(payoutRequest));
        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenAnswer(i -> i.getArguments()[0]);

        PayoutRequestResponse response = payoutService.markPaid(adminId, payoutRequest.getId(), review);

        assertNotNull(response);
        assertEquals("PAID", response.getStatus());
        assertEquals("REF123", response.getTransactionReference());

        verify(payoutRequestRepository).save(payoutRequest);
        verify(payoutAuditLogRepository).save(any(PayoutAuditLog.class));
    }

    @Test
    void markPaid_Idempotent_NoOp() {
        UUID adminId = UUID.randomUUID();
        AdminReviewPayoutRequest review = new AdminReviewPayoutRequest();
        payoutRequest.setStatus("PAID");

        when(payoutRequestRepository.findById(payoutRequest.getId())).thenReturn(Optional.of(payoutRequest));

        PayoutRequestResponse response = payoutService.markPaid(adminId, payoutRequest.getId(), review);

        assertNotNull(response);
        assertEquals("PAID", response.getStatus());

        verify(payoutRequestRepository, never()).save(any());
        verify(payoutAuditLogRepository, never()).save(any());
    }

    @Test
    void markPaid_RejectedState_ThrowsException() {
        UUID adminId = UUID.randomUUID();
        AdminReviewPayoutRequest review = new AdminReviewPayoutRequest();
        payoutRequest.setStatus("REJECTED");

        when(payoutRequestRepository.findById(payoutRequest.getId())).thenReturn(Optional.of(payoutRequest));

        AppException ex = assertThrows(AppException.class, () ->
                payoutService.markPaid(adminId, payoutRequest.getId(), review));

        assertEquals(FinanceErrorCode.INVALID_PAYOUT_STATUS_TRANSITION.getCode(), ex.getExceptionCode().getCode());
    }
}
