package com.demandlane.dto;

import jakarta.validation.constraints.NotNull;

public record BorrowRequest(
        @NotNull Long bookId,
        @NotNull Long memberId
) {}
