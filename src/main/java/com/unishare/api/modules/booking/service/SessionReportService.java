package com.unishare.api.modules.booking.service;

import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.booking.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SessionReportService {
    SessionReportRequestResponse createRequest(UUID mentorId, UUID bookingId, CreateReportRequestDto dto);
    SessionReportRequestResponse submit(UUID menteeId, UUID requestId, SubmitReportDto dto);
    SessionReportRequestResponse review(UUID mentorId, UUID requestId, ReviewReportDto dto);
    List<SessionReportRequestResponse> listForBooking(UUID bookingId, UUID userId);
    PageResponse<SessionReportRequestResponse> listForMentee(UUID menteeId, Pageable pageable);
    PageResponse<SessionReportRequestResponse> listForMentor(UUID mentorId, Pageable pageable);
}
