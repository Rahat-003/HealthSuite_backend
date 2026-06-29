package com.healthsuite.phr.dto.request;

import com.healthsuite.phr.enums.MealTiming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AddMedicationRequest(
        @NotBlank String drugName,
        // Supports patterns like "1+0+1", "1+1+1", "10mg", "500mg BD"
        @NotBlank @Pattern(
                regexp = "^[\\d+]+(?:\\+[\\d+]+)*$|^[\\d.]+\\s*\\w+.*$",
                message = "Dosage pattern must be like '1+0+1', '1+1+1', or '10mg'"
        )
        String dosagePattern,
        Integer durationDays,
        @NotNull MealTiming mealTiming,
        String notes
) {}
