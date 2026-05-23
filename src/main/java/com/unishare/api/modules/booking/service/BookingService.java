package com.unishare.api.modules.booking.service;

import com.unishare.api.modules.booking.dto.*;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    /** Gọi sau khi order chuyển paid — idempotent. */
    void ensureBookingForOrder(UUID orderId);

    List<BookingResponse> listForBuyer(UUID buyerId);

    List<BookingResponse> listForMentor(UUID mentorId);

    BookingResponse getById(UUID bookingId, UUID userId);

    BookingSessionResponse updateSession(UUID bookingId, UUID sessionId, UUID actorUserId, UpdateSessionRequest req);

    EvidenceResponse addEvidence(UUID bookingId, UUID sessionId, UUID userId, AddEvidenceRequest req);

    BookingResponse cancelBooking(UUID bookingId, UUID actorUserId, CancelBookingRequest req);

    BookingSessionResponse completeSession(UUID bookingId, UUID sessionId, UUID actorUserId);
}
