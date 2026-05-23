package com.unishare.api.modules.service.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.modules.service.dto.request.CreateReportRequest;
import com.unishare.api.modules.service.dto.request.ReviewReportRequest;
import com.unishare.api.modules.service.dto.response.ProgressReportResponse;
import com.unishare.api.modules.service.entity.ProgressReport;
import com.unishare.api.modules.service.entity.ReportStatus;
import com.unishare.api.modules.service.exception.ProgressReportErrorCode;
import com.unishare.api.modules.mentor.service.MentorService;
import com.unishare.api.modules.service.repository.ProgressReportRepository;
import com.unishare.api.modules.service.service.ProgressReportService;
import com.unishare.api.modules.user.dto.UserProfileResponse;
import com.unishare.api.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressReportServiceImpl implements ProgressReportService {

    private final ProgressReportRepository progressReportRepository;
    private final MentorService mentorProfileService;
    private final UserService userService;

    @Override
    @Transactional
    public ProgressReportResponse createReport(UUID menteeId, CreateReportRequest request) {
        if (menteeId.equals(request.getMentorId())) {
            throw new AppException(ProgressReportErrorCode.PROGRESS_REPORT_SELF_TARGET_NOT_ALLOWED,
                    "Mentee không thể tạo báo cáo cho chính mình");
        }
        if (!mentorProfileService.mentorProfileExists(request.getMentorId())) {
            throw new AppException(ProgressReportErrorCode.PROGRESS_REPORT_MENTOR_NOT_FOUND,
                    "Không tìm thấy mentor nhận báo cáo");
        }

        ProgressReport report = new ProgressReport();
        report.setMenteeId(menteeId);
        report.setMentorId(request.getMentorId());
        report.setTitle(request.getTitle());
        report.setContent(request.getContent());
        report.setAttachmentUrl(request.getAttachmentUrl());

        ProgressReport saved = progressReportRepository.save(report);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgressReportResponse> getMenteeReports(UUID menteeId, Pageable pageable) {
        return progressReportRepository.findByMenteeId(menteeId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgressReportResponse> getMentorReports(UUID mentorId, Pageable pageable) {
        return progressReportRepository.findByMentorId(mentorId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ProgressReportResponse reviewReport(UUID mentorId, UUID reportId, ReviewReportRequest request) {
        ProgressReport report = progressReportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ProgressReportErrorCode.PROGRESS_REPORT_NOT_FOUND,
                        "Không tìm thấy báo cáo này"));

        if (!report.getMentorId().equals(mentorId)) {
            throw new AppException(ProgressReportErrorCode.PROGRESS_REPORT_ACCESS_DENIED,
                    "Bạn không có quyền chấm báo cáo này");
        }

        if (request.getStatus() == ReportStatus.PENDING) {
            throw new AppException(ProgressReportErrorCode.PROGRESS_REPORT_INVALID_REVIEW_STATUS,
                    "Mentor không thể phản hồi với trạng thái PENDING");
        }

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new AppException(ProgressReportErrorCode.PROGRESS_REPORT_INVALID_STATE,
                    "Báo cáo này đã được chấm hoặc xử lý.");
        }

        report.setStatus(request.getStatus());
        report.setMentorFeedback(request.getMentorFeedback());
        ProgressReport saved = progressReportRepository.save(report);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressReportResponse getReportById(UUID userId, UUID reportId) {
        ProgressReport report = progressReportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ProgressReportErrorCode.PROGRESS_REPORT_NOT_FOUND,
                        "Không tìm thấy báo cáo này"));
        if (!report.getMenteeId().equals(userId) && !report.getMentorId().equals(userId)) {
            throw new AppException(ProgressReportErrorCode.PROGRESS_REPORT_ACCESS_DENIED,
                    "Bạn không có quyền xem báo cáo này");
        }
        return mapToResponse(report);
    }

    private ProgressReportResponse mapToResponse(ProgressReport report) {
        return ProgressReportResponse.builder()
                .id(report.getId())
                .menteeId(report.getMenteeId())
                .mentorId(report.getMentorId())
                .menteeName(resolveDisplayName(report.getMenteeId(), "Mentee"))
                .mentorName(resolveDisplayName(report.getMentorId(), "Mentor"))
                .title(report.getTitle())
                .content(report.getContent())
                .attachmentUrl(report.getAttachmentUrl())
                .status(report.getStatus())
                .mentorFeedback(report.getMentorFeedback())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private String resolveDisplayName(UUID userId, String fallback) {
        UserProfileResponse profile = userService.getProfile(userId);
        if (profile == null) {
            return fallback;
        }

        String fullName = String.join(" ",
                        profile.getFirstName() == null ? "" : profile.getFirstName().trim(),
                        profile.getLastName() == null ? "" : profile.getLastName().trim())
                .trim();

        return fullName.isBlank() ? fallback : fullName;
    }
}
