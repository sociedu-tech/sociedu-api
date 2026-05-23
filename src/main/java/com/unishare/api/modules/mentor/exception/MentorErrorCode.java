package com.unishare.api.modules.mentor.exception;

import com.unishare.api.common.dto.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MentorErrorCode implements ExceptionCode {
    MENTOR_NOT_FOUND(404, "MENTOR_NOT_FOUND"),
    INVALID_SEARCH_FILTER(400, "INVALID_SEARCH_FILTER"),
    PROFILE_ALREADY_VERIFIED(400, "PROFILE_ALREADY_VERIFIED"),
    PROFILE_INCOMPLETE(400, "PROFILE_INCOMPLETE");

    private final Integer code;
    private final String type;
}
