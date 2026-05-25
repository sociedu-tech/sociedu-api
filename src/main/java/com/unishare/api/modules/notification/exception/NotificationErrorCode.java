package com.unishare.api.modules.notification.exception;

import com.unishare.api.common.dto.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements ExceptionCode {
    NOTIFICATION_NOT_FOUND(404, "NOTIFICATION_NOT_FOUND"),
    NOTIFICATION_ACCESS_DENIED(403, "NOTIFICATION_ACCESS_DENIED");

    private final Integer code;
    private final String type;
}
