package com.unishare.api.modules.mentorapplication.exception;

import com.unishare.api.common.dto.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MentorApplicationErrorCode implements ExceptionCode {
    REQUEST_NOT_FOUND(404, "MENTOR_REQUEST_NOT_FOUND"),
    ALREADY_MENTOR(400, "ALREADY_MENTOR"),
    PENDING_REQUEST_EXISTS(409, "PENDING_MENTOR_REQUEST_EXISTS"),
    NOT_REJECTED(400, "MENTOR_REQUEST_NOT_REJECTED"),
    NOT_REVIEWABLE(400, "MENTOR_REQUEST_NOT_REVIEWABLE"),
    ALREADY_TERMINAL(400, "MENTOR_REQUEST_ALREADY_TERMINAL");

    private final Integer code;
    private final String type;
}
