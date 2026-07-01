package com.healthsuite.marketplace.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

public record CreateConsultationRequest(
    @Size(max = 5000) String problemText,
    @NotEmpty @Size(min = 1, max = 3) List<Long> doctorProfileIds,
    @Min(1) @Max(5) int refundWindowHours
) {}
