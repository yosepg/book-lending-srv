package com.demandlane.dto;

import java.time.Instant;

public record SuccessResponse<T>(
        T data,
        Instant timestamp
) {
    public static <T> SuccessResponse<T> of(T data) {
        return new SuccessResponse<>(data, Instant.now());
    }
}
