package com.healthsuite.marketplace.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DoctorRegistrationRequest(
    @NotBlank String specialty,
    @NotBlank String qualifications,
    @Min(0) @Max(60) int experienceYears,
    @NotBlank String licenseNumber,
    String bio,
    String hospitalAffiliation,
    String availabilityNote,
    @NotNull @DecimalMin("50") BigDecimal consultationFeeBdt
) {}
