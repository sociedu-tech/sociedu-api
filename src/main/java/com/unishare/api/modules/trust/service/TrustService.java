package com.unishare.api.modules.trust.service;

import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.trust.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TrustService {

    ModerationReportResponse createReport(UUID reporterId, CreateModerationReportRequest request);

    PageResponse<ModerationReportResponse> myReports(UUID reporterId, Pageable pageable);

    ModerationReportResponse addEvidence(UUID reporterId, UUID reportId, AddReportEvidenceRequest request);

    ModerationReportResponse resolveReport(UUID moderatorUserId, UUID reportId, ResolveReportRequest request);

    DisputeResponse createDispute(UUID userId, CreateDisputeRequest request);

    PageResponse<DisputeResponse> myDisputes(UUID userId, Pageable pageable);

    DisputeResponse resolveDispute(UUID moderatorUserId, UUID disputeId, ResolveDisputeRequest request);
}
