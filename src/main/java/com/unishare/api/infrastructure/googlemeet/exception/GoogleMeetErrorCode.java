package com.unishare.api.infrastructure.googlemeet.exception;

import com.unishare.api.common.dto.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GoogleMeetErrorCode implements ExceptionCode {
    NOT_CONFIGURED(503, "GOOGLE_MEET_NOT_CONFIGURED"),
    NOT_AUTHORIZED(403, "GOOGLE_MEET_NOT_AUTHORIZED"),
    CREATE_FAILED(502, "GOOGLE_MEET_CREATE_FAILED");

    private final Integer code;
    private final String type;
}
