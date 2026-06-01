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
import com.unishare.api.modules.finance.service.PayoutService;
import com.unishare.api.modules.mentor.entity.MentorProfile;
import com.unishare.api.modules.mentor.exception.MentorErrorCode;
import com.unishare.api.modules.mentor.repository.MentorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutServiceImpl implements PayoutService {

    private final PayoutRequestRepository payoutRequestRepository;
    private final PayoutAuditLogRepository payoutAuditLogRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public PayoutRequestResponse createPayoutRequest(UUID mentorId, CreatePayoutRequest request) {
        log.info("Creating payout request for mentor: {}", mentorId);

        // 1. Acquire Pessimistic Write Lock on Mentor Profile
        MentorProfile profile = mentorProfileRepository.findAndLockByUserId(mentorId)
                .orElseThrow(() -> new AppException(MentorErrorCode.MENTOR_NOT_FOUND, "Mentor profile not found"));

        // 2. Calculate Available Balance
        BigDecimal totalEarned = bookingRepository.calculateTotalEarnedByMentor(mentorId);
        BigDecimal totalWithdrawn = payoutRequestRepository.calculateTotalWithdrawnByMentor(mentorId);
        BigDecimal lockedBalance = payoutRequestRepository.calculateLockedBalanceByMentor(mentorId);
        BigDecimal availableBalance = totalEarned.subtract(totalWithdrawn).subtract(lockedBalance);

        if (request.getAmount().compareTo(availableBalance) > 0) {
            throw new AppException(FinanceErrorCode.INSUFFICIENT_BALANCE, 
                    "Insufficient balance. Available: " + availableBalance + ", Requested: " + request.getAmount());
        }

        // 3. Create Payout Request
        BigDecimal platformFeeRate = BigDecimal.valueOf(10.00); // Default 10% platform fee
        BigDecimal grossAmount = request.getAmount();
        BigDecimal netAmount = grossAmount.multiply(BigDecimal.ONE.subtract(platformFeeRate.divide(BigDecimal.valueOf(100.0))));

        PayoutRequest payout = new PayoutRequest();
        payout.setMentorId(mentorId);
        payout.setGrossAmount(grossAmount);
        payout.setPlatformFeeRate(platformFeeRate);
        payout.setNetAmount(netAmount);
        payout.setStatus("PENDING");
        payout.setBankName(request.getBankName());
        payout.setAccountNumber(request.getAccountNumber());
        payout.setAccountHolder(request.getAccountHolder());

        payout = payoutRequestRepository.save(payout);

        // 4. Log initial audit record
        logAudit(payout.getId(), mentorId, null, "PENDING", "Initial payout request submission");

        return mapToResponse(payout, true); // Return masked account number
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueSummaryResponse getRevenueSummary(UUID mentorId) {
        // Verify mentor exists
        mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new AppException(MentorErrorCode.MENTOR_NOT_FOUND, "Mentor profile not found"));

        BigDecimal totalEarned = bookingRepository.calculateTotalEarnedByMentor(mentorId);
        BigDecimal totalWithdrawn = payoutRequestRepository.calculateTotalWithdrawnByMentor(mentorId);
        BigDecimal lockedBalance = payoutRequestRepository.calculateLockedBalanceByMentor(mentorId);
        BigDecimal availableBalance = totalEarned.subtract(totalWithdrawn).subtract(lockedBalance);

        return RevenueSummaryResponse.builder()
                .totalEarned(totalEarned)
                .totalWithdrawn(totalWithdrawn)
                .lockedBalance(lockedBalance)
                .availableBalance(availableBalance)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayoutRequestResponse> getMentorPayoutRequests(UUID mentorId, Pageable pageable) {
        return payoutRequestRepository.findByMentorIdOrderByCreatedAtDesc(mentorId, pageable)
                .map(p -> mapToResponse(p, true));
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutRequestResponse getPayoutRequest(UUID mentorId, UUID payoutRequestId) {
        PayoutRequest payout = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.PAYOUT_REQUEST_NOT_FOUND, "Payout request not found"));

        if (!payout.getMentorId().equals(mentorId)) {
            throw new AppException(FinanceErrorCode.PAYOUT_ACCESS_DENIED, "Access denied to this payout request");
        }

        return mapToResponse(payout, true);
    }

    // --- Admin APIs ---

    @Override
    @Transactional(readOnly = true)
    public Page<PayoutRequestResponse> getAllPayoutRequests(Pageable pageable) {
        return payoutRequestRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(p -> mapToResponse(p, false)); // Admins see unmasked bank account info for processing
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayoutRequestResponse> getPayoutRequestsByStatus(String status, Pageable pageable) {
        return payoutRequestRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable)
                .map(p -> mapToResponse(p, false)); // Admins see unmasked bank account info
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutRequestResponse getAdminPayoutRequest(UUID payoutRequestId) {
        PayoutRequest payout = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.PAYOUT_REQUEST_NOT_FOUND, "Payout request not found"));
        return mapToResponse(payout, false);
    }

    @Override
    @Transactional
    public PayoutRequestResponse approvePayoutRequest(UUID adminId, UUID payoutRequestId) {
        PayoutRequest payout = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.PAYOUT_REQUEST_NOT_FOUND, "Payout request not found"));

        String oldStatus = payout.getStatus();
        if (!"PENDING".equalsIgnoreCase(oldStatus)) {
            throw new AppException(FinanceErrorCode.INVALID_PAYOUT_STATUS_TRANSITION, 
                    "Cannot approve payout request in status: " + oldStatus);
        }

        payout.setStatus("APPROVED");
        payout.setProcessedBy(adminId);
        payout.setProcessedAt(Instant.now());
        payout = payoutRequestRepository.save(payout);

        logAudit(payout.getId(), adminId, oldStatus, "APPROVED", "Payout request approved by admin");

        return mapToResponse(payout, false);
    }

    @Override
    @Transactional
    public PayoutRequestResponse rejectPayoutRequest(UUID adminId, UUID payoutRequestId, AdminReviewPayoutRequest reviewRequest) {
        PayoutRequest payout = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.PAYOUT_REQUEST_NOT_FOUND, "Payout request not found"));

        String oldStatus = payout.getStatus();
        if (!"PENDING".equalsIgnoreCase(oldStatus) && !"APPROVED".equalsIgnoreCase(oldStatus)) {
            throw new AppException(FinanceErrorCode.INVALID_PAYOUT_STATUS_TRANSITION, 
                    "Cannot reject payout request in status: " + oldStatus);
        }

        payout.setStatus("REJECTED");
        payout.setRejectReason(reviewRequest.getRejectReason());
        payout.setProcessedBy(adminId);
        payout.setProcessedAt(Instant.now());
        payout = payoutRequestRepository.save(payout);

        logAudit(payout.getId(), adminId, oldStatus, "REJECTED", reviewRequest.getRejectReason());

        return mapToResponse(payout, false);
    }

    @Override
    @Transactional
    public PayoutRequestResponse markPaid(UUID adminId, UUID payoutRequestId, AdminReviewPayoutRequest reviewRequest) {
        PayoutRequest payout = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.PAYOUT_REQUEST_NOT_FOUND, "Payout request not found"));

        String oldStatus = payout.getStatus();
        
        // Idempotency: if already PAID, return current state (no-op)
        if ("PAID".equalsIgnoreCase(oldStatus)) {
            return mapToResponse(payout, false);
        }

        // Bị chặn hoàn toàn từ trạng thái REJECTED hoặc FAILED
        if ("REJECTED".equalsIgnoreCase(oldStatus) || "FAILED".equalsIgnoreCase(oldStatus)) {
            throw new AppException(FinanceErrorCode.INVALID_PAYOUT_STATUS_TRANSITION, 
                    "Cannot pay a payout request that is already REJECTED or FAILED");
        }

        payout.setStatus("PAID");
        payout.setTransactionReference(reviewRequest.getTransactionReference());
        payout.setProcessedBy(adminId);
        payout.setProcessedAt(Instant.now());
        payout = payoutRequestRepository.save(payout);

        logAudit(payout.getId(), adminId, oldStatus, "PAID", "Payout completed successfully");

        return mapToResponse(payout, false);
    }

    @Override
    @Transactional
    public PayoutRequestResponse markFailed(UUID adminId, UUID payoutRequestId, AdminReviewPayoutRequest reviewRequest) {
        PayoutRequest payout = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new AppException(FinanceErrorCode.PAYOUT_REQUEST_NOT_FOUND, "Payout request not found"));

        String oldStatus = payout.getStatus();

        // Idempotency: if already FAILED, return current state
        if ("FAILED".equalsIgnoreCase(oldStatus)) {
            return mapToResponse(payout, false);
        }

        if ("PAID".equalsIgnoreCase(oldStatus) || "REJECTED".equalsIgnoreCase(oldStatus)) {
            throw new AppException(FinanceErrorCode.INVALID_PAYOUT_STATUS_TRANSITION, 
                    "Cannot mark failed on a payout request that is already PAID or REJECTED");
        }

        payout.setStatus("FAILED");
        payout.setFailureReason(reviewRequest.getFailureReason());
        payout.setProcessedBy(adminId);
        payout.setProcessedAt(Instant.now());
        payout = payoutRequestRepository.save(payout);

        logAudit(payout.getId(), adminId, oldStatus, "FAILED", reviewRequest.getFailureReason());

        return mapToResponse(payout, false);
    }

    private void logAudit(UUID payoutRequestId, UUID actorId, String previousStatus, String nextStatus, String reason) {
        PayoutAuditLog audit = new PayoutAuditLog();
        audit.setPayoutRequestId(payoutRequestId);
        audit.setActorId(actorId);
        audit.setPreviousStatus(previousStatus);
        audit.setNextStatus(nextStatus);
        audit.setReason(reason);
        payoutAuditLogRepository.save(audit);
    }

    private PayoutRequestResponse mapToResponse(PayoutRequest payout, boolean maskAccount) {
        String accountNumber = payout.getAccountNumber();
        if (maskAccount) {
            accountNumber = PayoutRequestResponse.maskAccountNumber(accountNumber);
        }

        return PayoutRequestResponse.builder()
                .id(payout.getId())
                .mentorId(payout.getMentorId())
                .grossAmount(payout.getGrossAmount())
                .platformFeeRate(payout.getPlatformFeeRate())
                .netAmount(payout.getNetAmount())
                .status(payout.getStatus())
                .bankName(payout.getBankName())
                .accountNumber(accountNumber)
                .accountHolder(payout.getAccountHolder())
                .rejectReason(payout.getRejectReason())
                .failureReason(payout.getFailureReason())
                .transactionReference(payout.getTransactionReference())
                .createdAt(payout.getCreatedAt())
                .updatedAt(payout.getUpdatedAt())
                .processedAt(payout.getProcessedAt())
                .build();
    }
}
