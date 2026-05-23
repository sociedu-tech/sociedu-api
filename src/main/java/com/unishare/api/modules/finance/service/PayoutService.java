package com.unishare.api.modules.finance.service;

import com.unishare.api.modules.finance.dto.AdminReviewPayoutRequest;
import com.unishare.api.modules.finance.dto.CreatePayoutRequest;
import com.unishare.api.modules.finance.dto.PayoutRequestResponse;
import com.unishare.api.modules.finance.dto.RevenueSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PayoutService {
    PayoutRequestResponse createPayoutRequest(UUID mentorId, CreatePayoutRequest request);
    RevenueSummaryResponse getRevenueSummary(UUID mentorId);
    Page<PayoutRequestResponse> getMentorPayoutRequests(UUID mentorId, Pageable pageable);
    PayoutRequestResponse getPayoutRequest(UUID mentorId, UUID payoutRequestId);
    
    // Admin APIs
    Page<PayoutRequestResponse> getAllPayoutRequests(Pageable pageable);
    Page<PayoutRequestResponse> getPayoutRequestsByStatus(String status, Pageable pageable);
    PayoutRequestResponse approvePayoutRequest(UUID adminId, UUID payoutRequestId);
    PayoutRequestResponse rejectPayoutRequest(UUID adminId, UUID payoutRequestId, AdminReviewPayoutRequest reviewRequest);
    PayoutRequestResponse markPaid(UUID adminId, UUID payoutRequestId, AdminReviewPayoutRequest reviewRequest);
    PayoutRequestResponse markFailed(UUID adminId, UUID payoutRequestId, AdminReviewPayoutRequest reviewRequest);
}
