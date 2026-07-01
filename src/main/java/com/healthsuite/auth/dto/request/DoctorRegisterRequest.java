package com.healthsuite.auth.dto.request;

import com.healthsuite.common.validation.BangladeshPhone;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DoctorRegisterRequest(
        // ── User account ──────────────────────────────────────────────────
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @BangladeshPhone String phoneNumber,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,

        // ── Doctor profile ────────────────────────────────────────────────
        @NotBlank String specialty,
        @NotBlank String qualifications,
        @Min(0) @Max(60) int experienceYears,
        @NotBlank String licenseNumber,
        String bio,
        String hospitalAffiliation,
        String availabilityNote,
        @NotNull @DecimalMin("50") BigDecimal consultationFeeBdt
) {}
