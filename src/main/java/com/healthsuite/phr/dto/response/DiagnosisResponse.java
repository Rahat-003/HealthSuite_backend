package com.healthsuite.phr.dto.response;

import com.healthsuite.phr.entity.Diagnosis;

public record DiagnosisResponse(Long id, String name, String type, String notes) {
    public static DiagnosisResponse from(Diagnosis d) {
        return new DiagnosisResponse(d.getId(), d.getName(), d.getType(), d.getNotes());
    }
}
