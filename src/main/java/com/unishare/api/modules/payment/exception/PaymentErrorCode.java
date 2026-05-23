package com.unishare.api.modules.payment.exception;

import com.unishare.api.common.dto.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements ExceptionCode {
    PAYMENT_NOT_FOUND(404, "PAYMENT_NOT_FOUND"),
    PAYMENT_FAILED(402, "PAYMENT_FAILED"),
    INVALID_SIGNATURE(400, "INVALID_SIGNATURE"),
    INVALID_CALLBACK(400, "INVALID_CALLBACK"),
    PAYMENT_ALREADY_PROCESSED(409, "PAYMENT_ALREADY_PROCESSED"),
    SIGNATURE_COMPUTATION_FAILED(500, "SIGNATURE_COMPUTATION_FAILED");

    private final Integer code;
    private final String type;
}
