package com.unishare.api.modules.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private UUID id;
    private UUID orderId;
    private UUID buyerId;
    private UUID mentorId;
    private UUID packageId;
    private String status;
    private Integer progressPercent;
    private Instant createdAt;
    private List<BookingSessionResponse> sessions;
}
