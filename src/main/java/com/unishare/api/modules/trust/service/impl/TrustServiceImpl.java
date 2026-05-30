package com.unishare.api.modules.trust.service.impl;

import com.unishare.api.common.constants.DisputeStatuses;
import com.unishare.api.common.constants.ReportStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.trust.dto.*;
import com.unishare.api.modules.trust.entity.Dispute;
import com.unishare.api.modules.trust.entity.ModerationReport;
import com.unishare.api.modules.trust.entity.ModerationReportEvidence;
import com.unishare.api.modules.trust.exception.TrustErrorCode;
import com.unishare.api.modules.trust.repository.DisputeRepository;
import com.unishare.api.modules.trust.repository.ModerationReportEvidenceRepository;
import com.unishare.api.modules.trust.repository.ModerationReportRepository;
import com.unishare.api.modules.trust.service.TrustService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrustServiceImpl implements TrustService {

    private final ModerationReportRepository reportRepository;
    private final ModerationReportEvidenceRepository evidenceRepository;
    private final DisputeRepository disputeRepository;

    @Override
    @Transactional
    public ModerationReportResponse createReport(UUID reporterId, CreateModerationReportRequest request) {
        ModerationReport r = new ModerationReport();
        r.setReporterId(reporterId);
        r.setReportedUserId(request.getReportedUserId());
        r.setType(request.getType());
        r.setEntityId(request.getEntityId());
        r.setReason(request.getReason());
        r.setDescription(request.getDescription());
        r.setStatus(ReportStatuses.OPEN);
        r = reportRepository.save(r);
        return toReportResponse(r);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ModerationReportResponse> myReports(UUID reporterId, Pageable pageable) {
        return PageResponse.of(reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId, pageable)
                .map(this::toReportResponse));
    }

    @Override
    @Transactional
    public ModerationReportResponse addEvidence(UUID reporterId, UUID reportId, AddReportEvidenceRequest request) {
        ModerationReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(TrustErrorCode.REPORT_NOT_FOUND));
        if (!r.getReporterId().equals(reporterId)) {
            throw new AppException(TrustErrorCode.TRUST_ACCESS_DENIED);
        }
        ModerationReportEvidence e = new ModerationReportEvidence();
        e.setReportId(reportId);
        e.setFileId(request.getFileId());
        e.setDescription(request.getDescription());
        e.setUploadedBy(reporterId);
        evidenceRepository.save(e);
        return toReportResponse(r);
    }

    @Override
    @Transactional
    public ModerationReportResponse resolveReport(UUID moderatorUserId, UUID reportId, ResolveReportRequest request) {
        ModerationReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(TrustErrorCode.REPORT_NOT_FOUND));
        r.setStatus(request.getStatus());
        r.setResolutionNote(request.getResolutionNote());
        r.setResolvedAt(Instant.now());
        r.setResolvedBy(moderatorUserId);
        reportRepository.save(r);
        return toReportResponse(r);
    }

    @Override
    @Transactional
    public DisputeResponse createDispute(UUID userId, CreateDisputeRequest request) {
        Dispute d = new Dispute();
        d.setReportId(request.getReportId());
        d.setBookingId(request.getBookingId());
        d.setSessionId(request.getSessionId());
        d.setRaisedBy(userId);
        d.setReason(request.getReason());
        d.setDescription(request.getDescription());
        d.setStatus(DisputeStatuses.OPEN);
        d = disputeRepository.save(d);
        return toDisputeResponse(d);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DisputeResponse> myDisputes(UUID userId, Pageable pageable) {
        return PageResponse.of(disputeRepository.findByRaisedByOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDisputeResponse));
    }

    @Override
    @Transactional
    public DisputeResponse resolveDispute(UUID moderatorUserId, UUID disputeId, ResolveDisputeRequest request) {
        Dispute d = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new AppException(TrustErrorCode.DISPUTE_NOT_FOUND));
        d.setStatus(request.getStatus());
        d.setResolutionNote(request.getResolutionNote());
        d.setResolvedAt(Instant.now());
        d.setResolvedBy(moderatorUserId);
        disputeRepository.save(d);
        return toDisputeResponse(d);
    }

    private ModerationReportResponse toReportResponse(ModerationReport r) {
        return ModerationReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReporterId())
                .reportedUserId(r.getReportedUserId())
                .type(r.getType())
                .entityId(r.getEntityId())
                .reason(r.getReason())
                .description(r.getDescription())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .resolvedAt(r.getResolvedAt())
                .resolvedBy(r.getResolvedBy())
                .resolutionNote(r.getResolutionNote())
                .build();
    }

    private DisputeResponse toDisputeResponse(Dispute d) {
        return DisputeResponse.builder()
                .id(d.getId())
                .reportId(d.getReportId())
                .bookingId(d.getBookingId())
                .sessionId(d.getSessionId())
                .raisedBy(d.getRaisedBy())
                .reason(d.getReason())
                .description(d.getDescription())
                .status(d.getStatus())
                .resolutionNote(d.getResolutionNote())
                .createdAt(d.getCreatedAt())
                .resolvedAt(d.getResolvedAt())
                .resolvedBy(d.getResolvedBy())
                .build();
    }
}
