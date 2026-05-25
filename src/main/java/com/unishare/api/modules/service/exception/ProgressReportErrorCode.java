package com.unishare.api.modules.service.exception;

import com.unishare.api.common.dto.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProgressReportErrorCode implements ExceptionCode {
    PROGRESS_REPORT_NOT_FOUND(404, "PROGRESS_REPORT_NOT_FOUND"),
    PROGRESS_REPORT_ACCESS_DENIED(403, "PROGRESS_REPORT_ACCESS_DENIED"),
    PROGRESS_REPORT_INVALID_REVIEW_STATUS(400, "PROGRESS_REPORT_INVALID_REVIEW_STATUS"),
    PROGRESS_REPORT_MENTOR_NOT_FOUND(404, "PROGRESS_REPORT_MENTOR_NOT_FOUND"),
    PROGRESS_REPORT_SELF_TARGET_NOT_ALLOWED(400, "PROGRESS_REPORT_SELF_TARGET_NOT_ALLOWED"),
    PROGRESS_REPORT_INVALID_STATE(400, "PROGRESS_REPORT_INVALID_STATE");

    private final Integer code;
    private final String type;
}
