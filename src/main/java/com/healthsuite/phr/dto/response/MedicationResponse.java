package com.healthsuite.phr.dto.response;

import com.healthsuite.phr.entity.Medication;
import com.healthsuite.phr.enums.MealTiming;

public record MedicationResponse(
        Long id,
        String drugName,
        String dosagePattern,
        Integer durationDays,
        MealTiming mealTiming,
        String notes
) {
    public static MedicationResponse from(Medication m) {
        return new MedicationResponse(m.getId(), m.getDrugName(), m.getDosagePattern(),
                m.getDurationDays(), m.getMealTiming(), m.getNotes());
    }
}
