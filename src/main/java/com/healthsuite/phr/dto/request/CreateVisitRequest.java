package com.healthsuite.phr.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.List;

public record CreateVisitRequest(
        @NotNull @PastOrPresent LocalDate visitDate,
        @NotBlank String doctorName,
        String doctorSpecialty,
        String hospitalName,
        String notes,
        @Valid List<DiagnosisRequest> diagnoses
) {
    public record DiagnosisRequest(
            @NotBlank String name,
            String type,  // DISEASE | SYMPTOM
            String notes
    ) {}
}
