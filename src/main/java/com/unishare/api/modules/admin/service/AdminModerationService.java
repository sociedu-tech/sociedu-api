package com.unishare.api.modules.admin.service;

import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.admin.dto.AdminModerationReportResponse;
import com.unishare.api.modules.admin.dto.AdminResolveModerationRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminModerationService {

    PageResponse<AdminModerationReportResponse> list(String segment, String status, Pageable pageable);

    AdminModerationReportResponse getById(UUID id);

    AdminModerationReportResponse resolve(UUID adminId, UUID reportId, AdminResolveModerationRequest request);
}
