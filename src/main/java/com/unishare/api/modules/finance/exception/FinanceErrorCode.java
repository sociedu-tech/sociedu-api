package com.unishare.api.modules.finance.exception;

import com.unishare.api.common.dto.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FinanceErrorCode implements ExceptionCode {
    INSUFFICIENT_BALANCE(400, "INSUFFICIENT_BALANCE"),
    INVALID_PAYOUT_AMOUNT(400, "INVALID_PAYOUT_AMOUNT"),
    PAYOUT_REQUEST_NOT_FOUND(404, "PAYOUT_REQUEST_NOT_FOUND"),
    INVALID_PAYOUT_STATUS_TRANSITION(400, "INVALID_PAYOUT_STATUS_TRANSITION"),
    PAYOUT_ACCESS_DENIED(403, "PAYOUT_ACCESS_DENIED");

    private final Integer code;
    private final String type;
}
