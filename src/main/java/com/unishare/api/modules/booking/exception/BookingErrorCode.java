package com.unishare.api.modules.booking.exception;

import com.unishare.api.common.dto.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BookingErrorCode implements ExceptionCode {
    BOOKING_NOT_FOUND(404, "BOOKING_NOT_FOUND"),
    BOOKING_ACCESS_DENIED(403, "BOOKING_ACCESS_DENIED"),
    SESSION_NOT_FOUND(404, "SESSION_NOT_FOUND"),
    INVALID_SCHEDULE_TIME(400, "INVALID_SCHEDULE_TIME"),
    INVALID_STATE_TRANSITION(400, "INVALID_STATE_TRANSITION"),
    BOOKING_CANNOT_CANCEL(400, "BOOKING_CANNOT_CANCEL"),
    REVIEW_ALREADY_EXISTS(409, "REVIEW_ALREADY_EXISTS"),
    BOOKING_NOT_COMPLETED(400, "BOOKING_NOT_COMPLETED"),
    REVIEW_ACCESS_DENIED(403, "REVIEW_ACCESS_DENIED");

    private final Integer code;
    private final String type;
}
