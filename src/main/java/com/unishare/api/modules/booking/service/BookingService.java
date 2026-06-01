package com.unishare.api.modules.booking.service;

import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.booking.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookingService {

    /** @return {@code true} nếu booking mới được tạo trong lần gọi này. */
    boolean ensureBookingForOrder(UUID orderId);

    PageResponse<BookingResponse> listForBuyer(UUID buyerId, Pageable pageable);

    PageResponse<BookingResponse> listForMentor(UUID mentorId, Pageable pageable);

    /** Buổi học sắp tới gần nhất của học viên (buyer); null nếu không có. */
    NextUpcomingSessionResponse getNextUpcomingSessionForBuyer(UUID buyerId);

    /** Buổi dạy sắp tới gần nhất của mentor; null nếu không có. */
    NextUpcomingSessionResponse getNextUpcomingSessionForMentor(UUID mentorId);

    BookingResponse getById(UUID bookingId, UUID userId);

    BookingSessionResponse updateSession(UUID bookingId, UUID sessionId, UUID actorUserId, UpdateSessionRequest req);

    EvidenceResponse addEvidence(UUID bookingId, UUID sessionId, UUID userId, AddEvidenceRequest req);

    BookingResponse cancelBooking(UUID bookingId, UUID actorUserId, CancelBookingRequest req);

    BookingSessionResponse completeSession(UUID bookingId, UUID sessionId, UUID actorUserId);

    BookingSessionResponse confirmSessionCompletion(
            UUID bookingId, UUID sessionId, UUID actorUserId, ConfirmSessionCompletionRequest req);
    BookingSessionResponse createSession(UUID bookingId, UUID mentorId, CreateSessionRequest req);

    BookingResponse updateProgress(UUID bookingId, UUID mentorId, int progressPercent);
}
