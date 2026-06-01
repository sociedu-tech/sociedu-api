package com.unishare.api.modules.chat.exception;

import com.unishare.api.common.dto.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements ExceptionCode {
    CONVERSATION_NOT_FOUND(404, "CONVERSATION_NOT_FOUND"),
    CHAT_ACCESS_DENIED(403, "CHAT_ACCESS_DENIED"),
    INVALID_CHAT_PEER(400, "INVALID_CHAT_PEER"),
    INVALID_MESSAGE_CONTEXT(400, "INVALID_MESSAGE_CONTEXT");

    private final Integer code;
    private final String type;
}
