package com.unishare.api.modules.order.exception;

import com.unishare.api.common.dto.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ExceptionCode {
    ORDER_NOT_FOUND(404, "ORDER_NOT_FOUND"),
    ORDER_NOT_PAYABLE(400, "ORDER_NOT_PAYABLE"),
    PAYMENT_FAILED(402, "PAYMENT_FAILED"),
    PAYMENT_INVALID_SIGNATURE(400, "PAYMENT_INVALID_SIGNATURE");

    private final Integer code;
    private final String type;
}
