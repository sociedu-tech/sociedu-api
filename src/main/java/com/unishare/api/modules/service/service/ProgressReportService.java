package com.unishare.api.modules.service.service;

import com.unishare.api.modules.service.dto.request.CreateReportRequest;
import com.unishare.api.modules.service.dto.request.ReviewReportRequest;
import com.unishare.api.modules.service.dto.response.ProgressReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProgressReportService {
    ProgressReportResponse createReport(UUID menteeId, CreateReportRequest request);

    Page<ProgressReportResponse> getMenteeReports(UUID menteeId, Pageable pageable);

    Page<ProgressReportResponse> getMentorReports(UUID mentorId, Pageable pageable);

    ProgressReportResponse reviewReport(UUID mentorId, UUID reportId, ReviewReportRequest request);

    ProgressReportResponse getReportById(UUID userId, UUID reportId);
}
