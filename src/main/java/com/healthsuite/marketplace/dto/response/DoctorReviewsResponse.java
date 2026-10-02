package com.healthsuite.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DoctorReviewsResponse(
    BigDecimal average,
    long total,
    Map<Integer, Long> breakdown,
    List<ReviewItem> reviews
) {
    /** patientName is display-safe: first name + last initial, never the full identity. */
    public record ReviewItem(
        int stars,
        String comment,
        String patientName,
        LocalDateTime createdAt
    ) {}
}
