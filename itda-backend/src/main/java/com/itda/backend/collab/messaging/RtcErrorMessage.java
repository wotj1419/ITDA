package com.itda.backend.collab.messaging;

import com.itda.backend.global.response.ErrorCode;

public record RtcErrorMessage(
        String code,
        String message
) {
    public static RtcErrorMessage from(ErrorCode errorCode) {
        if (errorCode == null) {
            return new RtcErrorMessage("UNKNOWN", "Unknown error");
        }
        return new RtcErrorMessage(errorCode.getCode(), errorCode.getMessage());
    }
}
