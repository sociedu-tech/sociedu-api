package com.unishare.api.modules.admin.service.impl;

import com.unishare.api.common.constants.ReportStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.admin.dto.AdminModerationReportResponse;
import com.unishare.api.modules.admin.dto.AdminResolveModerationRequest;
import com.unishare.api.modules.admin.service.AdminModerationService;
import com.unishare.api.modules.booking.entity.Booking;
import com.unishare.api.modules.booking.entity.BookingSession;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.booking.repository.BookingSessionRepository;
import com.unishare.api.modules.trust.dto.ResolveReportRequest;
import com.unishare.api.modules.trust.entity.Dispute;
import com.unishare.api.modules.trust.entity.ModerationReport;
import com.unishare.api.modules.trust.exception.TrustErrorCode;
import com.unishare.api.modules.trust.repository.DisputeRepository;
import com.unishare.api.modules.trust.repository.ModerationReportRepository;
import com.unishare.api.modules.trust.service.TrustService;
import com.unishare.api.modules.user.entity.UserProfile;
import com.unishare.api.modules.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminModerationServiceImpl implements AdminModerationService {

    private final ModerationReportRepository reportRepository;
    private final DisputeRepository disputeRepository;
    private final UserProfileRepository userProfileRepository;
    private final BookingRepository bookingRepository;
    private final BookingSessionRepository sessionRepository;
    private final TrustService trustService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminModerationReportResponse> list(String segment, String status, Pageable pageable) {
        Specification<ModerationReport> spec = segmentSpec(segment);
        String beStatus = mapFeStatusToBe(status);
        if (beStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), beStatus));
        }
        Page<ModerationReport> page = reportRepository.findAll(spec, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminModerationReportResponse getById(UUID id) {
        ModerationReport report = reportRepository.findById(id)
                .orElseThrow(() -> new AppException(TrustErrorCode.REPORT_NOT_FOUND));
        return toResponse(report);
    }

    @Override
    @Transactional
    public AdminModerationReportResponse resolve(UUID adminId, UUID reportId, AdminResolveModerationRequest request) {
        ResolveReportRequest resolve = new ResolveReportRequest();
        resolve.setStatus(mapFeStatusToBe(request.getStatus()));
        resolve.setResolutionNote(request.getResolutionNote());
        trustService.resolveReport(adminId, reportId, resolve);
        return getById(reportId);
    }

    private Specification<ModerationReport> segmentSpec(String segment) {
        if (segment == null || segment.isBlank() || "all".equalsIgnoreCase(segment)) {
            return (root, query, cb) -> cb.conjunction();
        }
        return switch (segment.toLowerCase()) {
            case "people" -> (root, query, cb) -> root.get("type").in("user", "mentor");
            case "review" -> (root, query, cb) -> cb.equal(root.get("type"), "review");
            case "session" -> (root, query, cb) -> cb.equal(root.get("type"), "session");
            default -> (root, query, cb) -> cb.conjunction();
        };
    }

    private AdminModerationReportResponse toResponse(ModerationReport r) {
        Map<UUID, UserProfile> profiles = loadProfiles(Set.of(r.getReporterId(), r.getReportedUserId()));
        UserProfile reporter = profiles.get(r.getReporterId());
        String targetType = normalizeTargetType(r.getType());
        AdminModerationReportResponse.SessionDisputeDetailDto disputeDetail = null;
        if ("session".equals(targetType)) {
            disputeDetail = buildSessionDispute(r);
        }
        return AdminModerationReportResponse.builder()
                .id(r.getId())
                .createdAt(r.getCreatedAt())
                .reporterId(r.getReporterId())
                .reporterName(reporter != null ? reporter.getDisplayName() : "Người báo cáo")
                .targetType(targetType)
                .targetLabel(buildTargetLabel(r))
                .category(r.getReason())
                .summary(r.getDescription() != null ? r.getDescription() : r.getReason())
                .status(mapBeStatusToFe(r.getStatus()))
                .priority(priorityOf(r))
                .sessionDispute(disputeDetail)
                .build();
    }

    private AdminModerationReportResponse.SessionDisputeDetailDto buildSessionDispute(ModerationReport r) {
        Dispute dispute = disputeRepository.findFirstByReportId(r.getId()).orElse(null);
        BookingSession session = null;
        Booking booking = null;
        if (r.getEntityId() != null) {
            session = sessionRepository.findById(r.getEntityId()).orElse(null);
            if (session != null) {
                booking = bookingRepository.findById(session.getBookingId()).orElse(null);
            }
        }
        if (booking == null && dispute != null && dispute.getBookingId() != null) {
            booking = bookingRepository.findById(dispute.getBookingId()).orElse(null);
        }
        UUID buyerId = booking != null ? booking.getBuyerId() : null;
        UUID mentorId = booking != null ? booking.getMentorId() : null;
        Map<UUID, UserProfile> profiles = loadProfiles(Set.of(buyerId, mentorId, r.getReporterId()));
        String menteeName = buyerId != null && profiles.get(buyerId) != null
                ? profiles.get(buyerId).getDisplayName() : "Học viên";
        String mentorName = mentorId != null && profiles.get(mentorId) != null
                ? profiles.get(mentorId).getDisplayName() : "Mentor";
        Instant sessionAt = session != null ? session.getScheduledAt() : r.getCreatedAt();
        String sessionCode = session != null
                ? "SS-" + session.getId().toString().substring(0, 8).toUpperCase()
                : "SS-UNKNOWN";
        String openedBy = "learner";
        if (dispute != null && dispute.getRaisedBy() != null && dispute.getRaisedBy().equals(mentorId)) {
            openedBy = "mentor";
        }
        String phase = mapPhase(r.getStatus());
        List<AdminModerationReportResponse.SessionDisputeStageDto> stages = List.of(
                stage("submitted", "Tiếp nhận", "Hệ thống ghi nhận khiếu nại.", true, r.getCreatedAt()),
                stage("evidence", "Minh chứng", "Các bên bổ sung tài liệu.", !ReportStatuses.OPEN.equalsIgnoreCase(r.getStatus()), null),
                stage("admin_review", "Admin xem xét", "Đội vận hành đánh giá.", ReportStatuses.UNDER_REVIEW.equalsIgnoreCase(r.getStatus())
                        || ReportStatuses.RESOLVED.equalsIgnoreCase(r.getStatus()), null),
                stage("decision", "Quyết định", "Kết luận và ghi chú.", ReportStatuses.RESOLVED.equalsIgnoreCase(r.getStatus())
                        || ReportStatuses.REJECTED.equalsIgnoreCase(r.getStatus()), r.getResolvedAt()),
                stage("closed", "Đóng hồ sơ", "Hoàn tất xử lý.", ReportStatuses.RESOLVED.equalsIgnoreCase(r.getStatus())
                        || ReportStatuses.REJECTED.equalsIgnoreCase(r.getStatus()), r.getResolvedAt())
        );
        List<AdminModerationReportResponse.SessionDisputeEvidenceDto> evidence = new ArrayList<>();
        if (dispute != null && dispute.getDescription() != null) {
            evidence.add(AdminModerationReportResponse.SessionDisputeEvidenceDto.builder()
                    .id(dispute.getId())
                    .party(openedBy)
                    .uploadedAt(dispute.getCreatedAt())
                    .title("Tường trình")
                    .detail(dispute.getDescription())
                    .fileLabel("dispute.txt")
                    .build());
        }
        return AdminModerationReportResponse.SessionDisputeDetailDto.builder()
                .sessionCode(sessionCode)
                .sessionAt(sessionAt)
                .menteeName(menteeName)
                .mentorName(mentorName)
                .openedByParty(openedBy)
                .openerStatement(dispute != null ? dispute.getReason() : r.getReason())
                .counterStatement(r.getDescription() != null ? r.getDescription() : "")
                .currentPhase(phase)
                .stages(stages)
                .evidence(evidence)
                .adminResolutionNote(r.getResolutionNote())
                .build();
    }

    private AdminModerationReportResponse.SessionDisputeStageDto stage(
            String phase, String label, String description, boolean done, Instant completedAt) {
        return AdminModerationReportResponse.SessionDisputeStageDto.builder()
                .phase(phase)
                .label(label)
                .description(description)
                .done(done)
                .completedAt(completedAt)
                .build();
    }

    private String mapPhase(String status) {
        if (ReportStatuses.RESOLVED.equalsIgnoreCase(status) || ReportStatuses.REJECTED.equalsIgnoreCase(status)) {
            return "closed";
        }
        if (ReportStatuses.UNDER_REVIEW.equalsIgnoreCase(status)) {
            return "admin_review";
        }
        return "submitted";
    }

    private String buildTargetLabel(ModerationReport r) {
        if (r.getEntityId() == null) {
            return r.getType();
        }
        return r.getType() + " · " + r.getEntityId().toString().substring(0, 8);
    }

    private String normalizeTargetType(String type) {
        if (type == null || type.isBlank()) {
            return "user";
        }
        return type.toLowerCase();
    }

    private String priorityOf(ModerationReport r) {
        String reason = r.getReason() != null ? r.getReason().toLowerCase() : "";
        if (reason.contains("gian lận") || reason.contains("quấy rối") || reason.contains("lừa")) {
            return "high";
        }
        if (reason.contains("spam")) {
            return "low";
        }
        return "normal";
    }

    private Map<UUID, UserProfile> loadProfiles(Set<UUID> ids) {
        Set<UUID> filtered = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (filtered.isEmpty()) {
            return Map.of();
        }
        return userProfileRepository.findAllById(filtered).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));
    }

    private String mapBeStatusToFe(String status) {
        if (status == null) {
            return "open";
        }
        return switch (status.toLowerCase()) {
            case "under_review" -> "in_review";
            case "rejected" -> "dismissed";
            case "resolved" -> "resolved";
            default -> "open";
        };
    }

    private String mapFeStatusToBe(String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return null;
        }
        return switch (status.toLowerCase()) {
            case "in_review" -> ReportStatuses.UNDER_REVIEW;
            case "dismissed" -> ReportStatuses.REJECTED;
            case "resolved" -> ReportStatuses.RESOLVED;
            default -> ReportStatuses.OPEN;
        };
    }
}
