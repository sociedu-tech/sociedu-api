package com.unishare.api.modules.booking.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.common.event.SessionReportRequestedEvent;
import com.unishare.api.common.event.SessionReportReviewedEvent;
import com.unishare.api.common.event.SessionReportSubmittedEvent;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.modules.booking.dto.*;
import com.unishare.api.modules.booking.entity.Booking;
import com.unishare.api.modules.booking.entity.SessionReportRequest;
import com.unishare.api.modules.booking.exception.BookingErrorCode;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.booking.repository.SessionReportRequestRepository;
import com.unishare.api.modules.booking.service.SessionReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionReportServiceImpl implements SessionReportService {

    private final SessionReportRequestRepository reportRequestRepository;
    private final BookingRepository bookingRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public SessionReportRequestResponse createRequest(UUID mentorId, UUID bookingId, CreateReportRequestDto dto) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
        if (!b.getMentorId().equals(mentorId)) {
            throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED, "Chỉ Mentor của Booking mới được tạo yêu cầu báo cáo.");
        }

        SessionReportRequest req = new SessionReportRequest();
        req.setBookingId(bookingId);
        req.setSessionId(dto.getSessionId());
        req.setMentorId(mentorId);
        req.setMenteeId(b.getBuyerId());
        req.setTitle(dto.getTitle());
        req.setDescription(dto.getDescription());
        req.setDueDate(dto.getDueDate());
        req.setStatus("PENDING_SUBMISSION");
        req = reportRequestRepository.save(req);

        eventPublisher.publish(new SessionReportRequestedEvent(
                req.getId(), bookingId, mentorId, b.getBuyerId(), req.getTitle()));

        return toResponse(req);
    }

    @Override
    @Transactional
    public SessionReportRequestResponse submit(UUID menteeId, UUID requestId, SubmitReportDto dto) {
        SessionReportRequest req = reportRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy yêu cầu báo cáo."));
        if (!req.getMenteeId().equals(menteeId)) {
            throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED, "Bạn không có quyền nộp báo cáo này.");
        }
        if (!"PENDING_SUBMISSION".equals(req.getStatus())) {
            throw new AppException(BookingErrorCode.INVALID_STATE_TRANSITION, "Báo cáo này không còn ở trạng thái chờ nộp.");
        }

        req.setMenteeContent(dto.getContent());
        req.setMenteeAttachmentUrl(dto.getAttachmentUrl());
        req.setStatus("SUBMITTED");
        req = reportRequestRepository.save(req);

        eventPublisher.publish(new SessionReportSubmittedEvent(
                req.getId(), req.getBookingId(), req.getMentorId(), menteeId, req.getTitle()));

        return toResponse(req);
    }

    @Override
    @Transactional
    public SessionReportRequestResponse review(UUID mentorId, UUID requestId, ReviewReportDto dto) {
        SessionReportRequest req = reportRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy yêu cầu báo cáo."));
        if (!req.getMentorId().equals(mentorId)) {
            throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED, "Bạn không có quyền duyệt báo cáo này.");
        }
        if (!"SUBMITTED".equals(req.getStatus())) {
            throw new AppException(BookingErrorCode.INVALID_STATE_TRANSITION, "Chỉ có thể duyệt báo cáo đã được nộp.");
        }
        String newStatus = "APPROVED".equalsIgnoreCase(dto.getStatus()) ? "APPROVED" : "REJECTED";
        req.setStatus(newStatus);
        req.setMentorFeedback(dto.getFeedback());
        req = reportRequestRepository.save(req);
        eventPublisher.publish(new SessionReportReviewedEvent(
                req.getId(), req.getBookingId(), req.getMentorId(), req.getMenteeId(), req.getTitle(), newStatus));
        return toResponse(req);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionReportRequestResponse> listForBooking(UUID bookingId, UUID userId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
        if (!b.getMentorId().equals(userId) && !b.getBuyerId().equals(userId)) {
            throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }
        return reportRequestRepository.findByBookingIdOrderByCreatedAtDesc(bookingId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SessionReportRequestResponse> listForMentee(UUID menteeId, Pageable pageable) {
        return PageResponse.of(reportRequestRepository.findByMenteeIdOrderByCreatedAtDesc(menteeId, pageable)
                .map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SessionReportRequestResponse> listForMentor(UUID mentorId, Pageable pageable) {
        return PageResponse.of(reportRequestRepository.findByMentorIdOrderByCreatedAtDesc(mentorId, pageable)
                .map(this::toResponse));
    }

    private SessionReportRequestResponse toResponse(SessionReportRequest r) {
        return SessionReportRequestResponse.builder()
                .id(r.getId())
                .bookingId(r.getBookingId())
                .sessionId(r.getSessionId())
                .mentorId(r.getMentorId())
                .menteeId(r.getMenteeId())
                .title(r.getTitle())
                .description(r.getDescription())
                .dueDate(r.getDueDate())
                .status(r.getStatus())
                .menteeContent(r.getMenteeContent())
                .menteeAttachmentUrl(r.getMenteeAttachmentUrl())
                .mentorFeedback(r.getMentorFeedback())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
