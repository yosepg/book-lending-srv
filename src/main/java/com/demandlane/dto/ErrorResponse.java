package com.demandlane.dto;

import java.time.Instant;

public record ErrorResponse(
        ErrorCode errorCode,
        String errorMessage,
        Instant timestamp
) {
    public static ErrorResponse of(ErrorCode errorCode, String errorMessage) {
        return new ErrorResponse(errorCode, errorMessage, Instant.now());
    }
}
