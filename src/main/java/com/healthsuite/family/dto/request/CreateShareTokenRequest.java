package com.healthsuite.family.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateShareTokenRequest(
        @NotBlank String recordType,
        @NotNull Long recordId,
        @Min(1) @Max(168) int ttlHours  // 1 hour min, 7 days max
) {
    public CreateShareTokenRequest {
        if (ttlHours == 0) ttlHours = 24; // default 24h
    }
}
