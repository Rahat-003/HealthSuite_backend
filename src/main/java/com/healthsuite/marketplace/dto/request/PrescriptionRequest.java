package com.healthsuite.marketplace.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PrescriptionRequest(
    @Size(max = 4000) String diagnosis,
    @Size(max = 4000) String remarks,
    @Min(1) @Max(365) Integer followUpDays,
    @Valid List<MedicineItem> medicines,
    @Valid List<TestItem> tests
) {
    public record MedicineItem(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 100) String dosage,
        @Size(max = 100) String frequency,
        @Size(max = 100) String duration,
        @Size(max = 300) String instructions
    ) {}

    public record TestItem(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 300) String note
    ) {}
}
